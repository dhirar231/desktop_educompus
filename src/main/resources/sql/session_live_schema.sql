-- ============================================================
-- Schema : Module Session Live Avancé
-- Database: educompus (MySQL)
-- Tables  : session_live, session_participants, session_actions
-- ============================================================

-- Table principale des sessions live
CREATE TABLE IF NOT EXISTS session_live (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    nom_cours        VARCHAR(255) NOT NULL COMMENT 'Nom du cours associé',
    lien             VARCHAR(512) NOT NULL COMMENT 'URL plateforme externe (Meet, Zoom…)',
    date             DATE         NOT NULL COMMENT 'Date prévue de la session',
    heure            TIME         NOT NULL COMMENT 'Heure de début prévue',
    statut           ENUM('PLANIFIEE','EN_COURS','TERMINEE','ANNULEE')
                         NOT NULL DEFAULT 'PLANIFIEE',
    cours_id         INT NULL     COMMENT 'FK vers cours (optionnel)',
    date_creation    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME   NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_sl_lien_non_vide     CHECK (CHAR_LENGTH(TRIM(lien)) > 0),
    CONSTRAINT chk_sl_nom_cours_non_vide CHECK (CHAR_LENGTH(TRIM(nom_cours)) > 0),

    FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE SET NULL,

    INDEX idx_sl_date    (date),
    INDEX idx_sl_statut  (statut),
    INDEX idx_sl_cours   (cours_id),
    INDEX idx_sl_date_statut (date, statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Sessions live de visioconférence';


-- Table des participants à une session
CREATE TABLE IF NOT EXISTS session_participants (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    session_id       INT          NOT NULL COMMENT 'FK vers session_live',
    etudiant_id      INT          NOT NULL COMMENT 'ID de l étudiant',
    nom_etudiant     VARCHAR(255) NOT NULL COMMENT 'Nom affiché',
    heure_join       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    heure_leave      DATETIME     NULL     COMMENT 'NULL = encore présent',
    main_levee       TINYINT(1)   NOT NULL DEFAULT 0,
    demande_parole   TINYINT(1)   NOT NULL DEFAULT 0,
    parole_accordee  TINYINT(1)   NOT NULL DEFAULT 0,

    FOREIGN KEY (session_id) REFERENCES session_live(id) ON DELETE CASCADE,

    -- Un étudiant ne peut être présent qu'une fois par session
    UNIQUE KEY uq_session_etudiant (session_id, etudiant_id),

    INDEX idx_sp_session  (session_id),
    INDEX idx_sp_etudiant (etudiant_id),
    INDEX idx_sp_present  (session_id, heure_leave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Participants aux sessions live';


-- Table du journal des actions/événements
CREATE TABLE IF NOT EXISTS session_actions (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    session_id   INT          NOT NULL COMMENT 'FK vers session_live',
    etudiant_id  INT          NOT NULL DEFAULT 0 COMMENT '0 = action système',
    nom_etudiant VARCHAR(255) NOT NULL DEFAULT 'Système',
    type         ENUM(
                     'JOIN','LEAVE',
                     'RAISE_HAND','LOWER_HAND','REQUEST_SPEAK',
                     'GRANT_SPEAK','REVOKE_SPEAK',
                     'SESSION_START','SESSION_END'
                 ) NOT NULL,
    timestamp    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details      TEXT         NULL     COMMENT 'Informations complémentaires',

    FOREIGN KEY (session_id) REFERENCES session_live(id) ON DELETE CASCADE,

    INDEX idx_sa_session   (session_id),
    INDEX idx_sa_etudiant  (session_id, etudiant_id),
    INDEX idx_sa_type      (session_id, type),
    INDEX idx_sa_timestamp (session_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Journal des événements des sessions live';
