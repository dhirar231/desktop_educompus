package com.educompus.web;

import com.educompus.model.ExamAnswer;
import com.educompus.model.ExamCatalogueItem;
import com.educompus.model.ExamQuestion;
import com.educompus.repository.ExamRepository;
// AI feedback removed per user request

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class EmbeddedExamServer {

private final ExamRepository repository = new ExamRepository();
private final HttpServer server;

public EmbeddedExamServer(int port) throws IOException {
server = HttpServer.create(new InetSocketAddress(port), 0);
server.createContext("/exam/take", new TakeHandler());
server.createContext("/exam/submit", new SubmitHandler());
server.setExecutor(Executors.newCachedThreadPool());
}

public void start() {
server.start();
}

public void stop(int delaySeconds) {
server.stop(delaySeconds);
}

private class TakeHandler implements HttpHandler {
@Override
public void handle(HttpExchange exchange) throws IOException {
if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
sendOptions(exchange);
return;
}
if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
sendText(exchange, 405, "Method Not Allowed");
return;
}

String path = exchange.getRequestURI().getPath();
int examId = parseId(path, "/exam/take/");
if (examId <= 0) {
sendText(exchange, 400, "Invalid exam id");
return;
}

List<ExamQuestion> questions;
try {
questions = repository.listQuestionsByExamId(examId);
} catch (Exception e) {
sendText(exchange, 500, "Failed to load exam: " + e.getMessage());
return;
}

			StringBuilder sb = new StringBuilder();
			sb.append("<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
					.append("<title>Examen ").append(examId).append("</title>")
					.append("<style>")
					.append("body{font-family:Arial,Helvetica,sans-serif;background:#f7f8fb;margin:0;padding:0}")
					.append(".container{max-width:900px;margin:0 auto;padding:14px}")
					.append(".header{display:flex;align-items:center;justify-content:space-between;padding:8px 0}")
					.append("h1{font-size:20px;margin:0}")
					.append("#timer{font-weight:900;background:#066ac9;color:#fff;padding:6px 12px;border-radius:8px;min-width:72px;text-align:center;display:inline-block;}")
					.append(".email-input input{width:100%;padding:10px;font-size:16px;border-radius:8px;border:1px solid #ddd}")
					.append(".question-card{margin:12px 0;padding:12px;border-radius:10px;border:1px solid #e7e7ee;background:#fff}")
					.append(".choices label{display:flex;align-items:center;gap:10px;padding:8px;border-radius:6px}")
					.append("input[type=radio]{width:18px;height:18px}")
					.append(".submit-btn{display:inline-block;padding:12px 18px;background:#1d7cf2;color:#fff;border:none;border-radius:8px;font-weight:600}")
					.append(".overlay{position:fixed;inset:0;background:rgba(0,0,0,0.6);display:none;align-items:center;justify-content:center;color:#fff;padding:20px}")
					.append(".overlay .box{background:#fff;color:#000;padding:18px;border-radius:10px;max-width:420px;text-align:center}")
					.append(".small-note{color:#666;font-size:13px;margin-top:8px}")
					.append("</style></head><body>");

			sb.append("<div class=\"container\">\n");
			sb.append("<div class=\"header\"><h1>Examen #").append(examId).append("</h1><div id=\"timer\">00:00</div></div>");
			sb.append("<form id=\"examForm\" method=\"post\" action=\"/exam/submit/").append(examId).append("\">");
			sb.append("<div class=\"email-input\"><label>Email (obligatoire):</label><input type=\"email\" name=\"email\" required placeholder=\"votre@email\" autocomplete=\"email\"></div>");

			for (ExamQuestion q : questions) {
				sb.append("<div class=\"question-card\" data-qid=\"").append(q.getId()).append("\" data-duration=\"").append(q.getDurationSeconds()).append("\">");
				sb.append("<div class=\"question-text\">\n").append(escapeHtml(q.getText())).append("</div>");
				sb.append("<div class=\"choices\">\n");
				List<ExamAnswer> answers = q.getAnswers();
				int idx = 0;
				for (ExamAnswer a : answers) {
					sb.append("<label><input type=\"radio\" name=\"q_").append(q.getId()).append("\" value=\"").append(idx).append("\"> ")
							.append(escapeHtml(a.getText())).append("</label>");
					idx++;
				}
				sb.append("</div>");
				sb.append("</div>");
			}

			    sb.append("<div id=\"navControls\" style=\"margin-top:16px;display:flex;gap:8px;\">")
				    .append("<button type=\"button\" id=\"prevBtn\" class=\"submit-btn\" style=\"background:#ccc;display:none\">Précédent</button>")
				    .append("<button type=\"button\" id=\"nextBtn\" class=\"submit-btn\" style=\"background:#1d7cf2\">Suivant</button>")
				    .append("<button type=\"submit\" id=\"submitBtn\" class=\"submit-btn\" style=\"display:none\">Soumettre</button>")
				    .append("</div>");
			sb.append("<div class=\"small-note\">Le bouton retour est masqué sur mobile pour éviter la reprise.</div>");
			sb.append("</form>\n</div>");

			sb.append("<div id=\"doneOverlay\" class=\"overlay\"><div class=\"box\"><h2>Vous avez déjà passé cet examen</h2><p>Merci. Vous ne pouvez pas repasser cet examen depuis cet appareil.</p><div style=\"margin-top:12px\"><button onclick=\"window.location.href='/'\" class=\"submit-btn\">Fermer</button></div></div></div>");

												String script = """
												<script>
												(function(){
													var examKey='educompus_exam_done_%d';
													try{
														if(localStorage.getItem(examKey)){
															document.getElementById('doneOverlay').style.display='flex';
															var f = document.getElementById('examForm'); if(f) f.style.display='none';
															return;
														}
													}catch(e){}

													var qCards = Array.prototype.slice.call(document.querySelectorAll('.question-card'));
													var timerEl = document.getElementById('timer');
													var emailInput = document.querySelector('.email-input input[name="email"]');
													var params = new URLSearchParams(location.search);
													var emailParam = params.get('email') || params.get('mail');
													if (emailParam && emailInput) {
														try { emailInput.value = decodeURIComponent(emailParam); emailInput.readOnly = true; } catch (e) { emailInput.value = emailParam; emailInput.readOnly = true; }
													}
													if (emailInput) emailInput.setAttribute('autocomplete','email');

													var current = 0;
													function show(idx){
														qCards.forEach(function(c,i){ c.style.display = i===idx ? 'block' : 'none'; });
														var prev = document.getElementById('prevBtn');
														var next = document.getElementById('nextBtn');
														var submit = document.getElementById('submitBtn');
														if(prev) prev.style.display = idx===0 ? 'none' : 'inline-block';
														if(next) next.style.display = idx>=qCards.length-1 ? 'none' : 'inline-block';
														if(submit) submit.style.display = idx>=qCards.length-1 ? 'inline-block' : 'none';
														startTimerFor(idx);
													}
													function startTimerFor(idx){
														var dur = parseInt(qCards[idx].getAttribute('data-duration')) || 45;
														var remaining = dur;
														updateTimer(remaining);
														if(window._examTimer) clearInterval(window._examTimer);
														window._examTimer = setInterval(function(){
															remaining--; if(remaining<0) remaining=0; updateTimer(remaining);
															if(remaining<=0){ clearInterval(window._examTimer);
																if(current < qCards.length-1){ current++; show(current); } else { var s = document.getElementById('submitBtn'); if(s) s.click(); }
															}
														},1000);
													}
													function updateTimer(sec){ if(!timerEl) return; var mm=Math.floor(sec/60); var ss=sec%60; timerEl.textContent = (mm<10? '0'+mm:mm)+':' + (ss<10? '0'+ss:ss); }
													var p = document.getElementById('prevBtn'); if(p) p.addEventListener('click', function(){ current = Math.max(0,current-1); show(current); });
													var n = document.getElementById('nextBtn'); if(n) n.addEventListener('click', function(){ current = Math.min(qCards.length-1,current+1); show(current); });
													if(qCards.length>0){ qCards.forEach(function(c){ c.style.display='none'; }); show(0); }
												})();
												</script>
												""".formatted(examId);

												sb.append(script);

												sb.append("</body></html>");

			sendHtml(exchange, 200, sb.toString());
}
}

private class SubmitHandler implements HttpHandler {
@Override
public void handle(HttpExchange exchange) throws IOException {
if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
sendOptions(exchange);
return;
}
if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
sendText(exchange, 405, "Method Not Allowed");
return;
}

String path = exchange.getRequestURI().getPath();
int examId = parseId(path, "/exam/submit/");
if (examId <= 0) {
sendText(exchange, 400, "Invalid exam id");
return;
}

String body = readAll(exchange.getRequestBody());
Map<String, List<String>> params = parseForm(body);
String email = single(params.get("email"));
if (email == null || email.isBlank()) {
sendText(exchange, 400, "Email required");
return;
}

List<ExamQuestion> questions;
try {
questions = repository.listQuestionsByExamId(examId);
} catch (Exception e) {
sendText(exchange, 500, "Failed to load exam: " + e.getMessage());
return;
}

int total = 0;
int correct = 0;
// per-question correctness map
Map<Integer, Boolean> correctByQuestion = new LinkedHashMap<>();
for (ExamQuestion q : questions) {
	total++;
	String key = "q_" + q.getId();
	List<String> vals = params.getOrDefault(key, Collections.emptyList());
	boolean qCorrect = false;
	if (!vals.isEmpty()) {
		try {
			int sel = Integer.parseInt(vals.get(0));
			if (sel >= 0 && sel < q.getAnswers().size() && q.getAnswers().get(sel).isCorrect()) {
				qCorrect = true;
				correct++;
			}
		} catch (NumberFormatException ignored) {
		}
	}
	correctByQuestion.put(q.getId(), qCorrect);
}

int percent = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
boolean passed = percent >= 50;
try {
	repository.recordAttempt(email, examId, percent, passed, null);
} catch (Exception ignored) {
}

				StringBuilder sb = new StringBuilder();
				sb.append("<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Résultat</title>");
				sb.append("<style>body{font-family:Arial,Helvetica,sans-serif;padding:14px} .stat{font-size:18px;margin:10px 0}.small-note{color:#666;font-size:13px;margin-top:8px}</style>");
				sb.append("</head><body>");
				sb.append("<h1>Résultat</h1>");
				sb.append("<div class=\"stat\">Score: ").append(percent).append("%</div>");
				sb.append("<div class=\"stat\">Réussite: ").append(passed ? "Oui" : "Non").append("</div>");
				sb.append("<div class=\"small-note\">Vous pouvez fermer cet onglet ou revenir à l'application.</div>");

				// mark localStorage on the phone if passed so user cannot retake on same device
				sb.append("<script>(function(){try:\n");
				if (passed) {
					sb.append("localStorage.setItem('educompus_exam_done_").append(examId).append("','1');\n");
				}
				sb.append("}catch(e){} })();</script>");
				sb.append("</body></html>");

				sendHtml(exchange, 200, sb.toString());
}
}

private static int parseId(String path, String prefix) {
try {
int idx = path.indexOf(prefix);
if (idx < 0) return -1;
String tail = path.substring(idx + prefix.length());
int slash = tail.indexOf('/');
if (slash > 0) tail = tail.substring(0, slash);
int q = tail.indexOf('?');
if (q > 0) tail = tail.substring(0, q);
return Integer.parseInt(tail);
} catch (Exception e) {
return -1;
}
}

private static void sendOptions(HttpExchange exchange) throws IOException {
Headers h = exchange.getResponseHeaders();
h.add("Access-Control-Allow-Origin", "*");
h.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
h.add("Access-Control-Allow-Headers", "Content-Type");
exchange.sendResponseHeaders(204, -1);
exchange.close();
}

private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
byte[] b = text.getBytes(StandardCharsets.UTF_8);
Headers h = exchange.getResponseHeaders();
h.add("Content-Type", "text/plain; charset=utf-8");
h.add("Access-Control-Allow-Origin", "*");
exchange.sendResponseHeaders(status, b.length);
try (OutputStream os = exchange.getResponseBody()) {
os.write(b);
}
}

private static void sendHtml(HttpExchange exchange, int status, String html) throws IOException {
byte[] b = html.getBytes(StandardCharsets.UTF_8);
Headers h = exchange.getResponseHeaders();
h.add("Content-Type", "text/html; charset=utf-8");
h.add("Access-Control-Allow-Origin", "*");
exchange.sendResponseHeaders(status, b.length);
try (OutputStream os = exchange.getResponseBody()) {
os.write(b);
}
}

private static String readAll(InputStream in) throws IOException {
return new String(in.readAllBytes(), StandardCharsets.UTF_8);
}

private static Map<String, List<String>> parseForm(String body) throws IOException {
if (body == null || body.isEmpty()) return Collections.emptyMap();
Map<String, List<String>> out = new HashMap<>();
String[] pairs = body.split("&");
for (String p : pairs) {
int eq = p.indexOf('=');
String k, v;
if (eq >= 0) {
k = URLDecoder.decode(p.substring(0, eq), StandardCharsets.UTF_8);
v = URLDecoder.decode(p.substring(eq + 1), StandardCharsets.UTF_8);
} else {
k = URLDecoder.decode(p, StandardCharsets.UTF_8);
v = "";
}
out.computeIfAbsent(k, x -> new ArrayList<>()).add(v);
}
return out;
}

private static String single(List<String> list) {
return (list == null || list.isEmpty()) ? null : list.get(0);
}

private static String escapeHtml(String s) {
if (s == null) return "";
return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
}
}
