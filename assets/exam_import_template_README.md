Exam import template

Files included:
- `exam_import_template.csv` — CSV template you can edit in Excel (Save as .xlsx after editing if desired).
- `exam_import_template.xls` — XML Spreadsheet (Excel 2003 XML) that Excel will open directly.

Columns (in order):
1. Exam Title — Name of the exam. Rows with the same Exam Title will be grouped into the same exam.
2. Exam Description — Short description for the exam.
3. Exam Level — A free-form label (e.g., "1A", "2B").
4. Course Id — Numeric id of the course to attach the exam to. If left empty or 0, you will be prompted to choose a course during import.
5. Question Text — The question's text.
6. Duration — Duration per question in seconds (e.g., `70`) or `MM:SS` format (e.g., `1:10`).
7. Choice 1
8. Choice 2
9. Choice 3
10. Choice 4
11. Correct — One of A/B/C/D or 1/2/3/4 pointing to the correct choice.

Notes:
- The importer expects Excel (.xls/.xlsx) files; you can edit the CSV in Excel and save as `.xlsx` before importing.
- If an exam with the same title already exists, the importer will still create a new exam (titles are not used to update existing exams).
- Use the `Course Id` column to avoid being prompted; otherwise the UI will ask you to select a course during import.

Example rows are included in the templates.
