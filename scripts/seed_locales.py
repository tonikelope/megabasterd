#!/usr/bin/env python3
"""Seed all locale bundles with translations for keys added by the 8.37
i18n refactor (legacy gaps + .form gaps + ui.* keys).

For each locale in DE/IT/HU/TR/ZH/VI, append any missing keys to
megabasterd-desktop/src/main/resources/i18n/messages_<locale>.properties.
Existing keys are left untouched -- this script is idempotent on re-run.

Translations seeded here are AI-assisted and need a human pass from the
language community on https://github.com/tonikelope/megabasterd/issues/397.

Spanish (ES) is already populated by direct manual edits and is skipped.
"""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BUNDLE_DIR = ROOT / "megabasterd-desktop" / "src" / "main" / "resources" / "i18n"

# English source for each key, used to emit "# EN: ..." comments and to
# guarantee parity if a translation map is missing a key (we fall back to
# leaving the entry out so ResourceBundle falls back to the English bundle).
EN = {
    # --- Section A: legacy gaps (translate("literal") that never had bundle entries)
    "warning_caps": "WARNING",
    "current_download_chunks_reset": "CURRENT DOWNLOAD CHUNKS RESET!",
    "error_doing_drag_and_drop_with_this_file":
        "ERROR DOING DRAG AND DROP WITH THIS FILE (use button method)",
    "error_checking_account_quota_no_excl": "ERROR checking account quota",
    "copy_email": "Copy email",
    "file_s_successfully_splitted": "File/s successfully splitted!",
    "mega_folder_temporarily_unavailable": "MEGA FOLDER TEMPORARILY UNAVAILABLE!",
    "mega_folder_blocked_deleted": "MEGA FOLDER BLOCKED/DELETED",
    "mega_folder_link_error": "MEGA FOLDER LINK ERROR!",
    "could_not_import_previous_version_settings":
        "Could not import the previous-version settings.",
    "import_failed": "Import failed",
    "file_grabber": "File Grabber",
    "accounts_successfully_reset": "Accounts successfully reset!",
    "accounts_reset": "Accounts reset",
    "there_are_no_accounts_to_export": "There are no accounts to export.",
    "export_accounts": "Export accounts",
    "exported_file_plain_text_warning":
        "The exported file will contain your credentials in PLAIN TEXT. Anyone with access to the file can use your accounts.\n\nContinue?",
    "file_already_exists_overwrite": "File already exists. Overwrite?",
    "using_this_option_corrupt_uploads":
        "Using this option may irreversibly corrupt your uploads.\n\nUSE IT AT YOUR OWN RISK",
    "txt_file_format": "TXT FILE FORMAT",

    # --- Section C: .form gaps (NetBeans-defined English never in legacy map)
    "zero_for_permanent_ban": "(0 for permanent ban)",
    "if_proxies_sorted_check_sequential":
        "(If you have a list of proxies sorted from best to worst, check sequential)",
    "lower_values_speed_up_proxies":
        "(Lower values can speed up finding working proxies but it could ban slow proxies)",
    "useful_to_avoid_slow_proxies":
        "(Useful to avoid getting trapped in slow proxies)",
    "two_fa_code": "2FA CODE",
    "copy_all_download_links": "COPY ALL DOWNLOAD LINKS",
    "copy_all_upload_links": "COPY ALL UPLOAD LINKS",
    "create_upload_folder_public_link": "CREATE UPLOAD FOLDER PUBLIC LINK",
    "check_version": "Check version",
    "always_reload_mega_folders":
        "Always reload MEGA folders instead of using cached folder data",
    "create_upload_thumbnails": "Create and upload image/video thumbnails",
    "create_upload_logs": "Create upload logs",
    "dark_mode": "DARK MODE",
    "force_all_current_chunks_reset": "FORCE ALL CURRENT CHUNKS RESET",
    "force_smart_proxy": "FORCE SMART PROXY",
    "file_merger": "File Merger",
    "file_splitter": "File Splitter",
    "folder_link": "FolderLink",
    "forces_smart_proxy_use":
        "Forces the use of smart proxy even if we still have direct bandwidth available (useful to test proxies)",
    "host_colon": "Host:",
    "master_password_setup": "Master password setup",
    "master_password_unlock": "Master password unlock",
    "note1_enable_mitigate_bandwidth":
        "Note1: enable it in order to mitigate bandwidth limit. (Multislot is required)",
    "note_slots_consume_resources":
        "Note: slots consume resources, so use them moderately.",
    "proxy_error_ban_time": "Proxy error ban time (seconds):",
    "proxy_list_refresh": "Proxy list refresh (minutes):",
    "proxy_selection_order": "Proxy selection order:",
    "proxy_timeout": "Proxy timeout (seconds):",
    "random_upper": "RANDOM",
    "reset_slot_proxy_after_chunk":
        "Reset slot proxy after successfully downloading a chunk",
    "sequential_upper": "SEQUENTIAL",
    "save_debug_info_to_file": "Save debug info to file ->",
    "speed_upper": "Speed",
    "streamer": "Streamer",
    "this_option_not_recommended_upload_folder":
        "THIS OPTION IS NOT RECOMMENDED. Using this will cause MegaBasterd uploaded folder to appear in your account as NOT DECRYPTABLE. \n\nAt the time of writing this text, there is a method to FIX IT:\n\n1) Move first upload subfolder to the ROOT (CLOUD) folder of your account. \n\n2) Go to account settings and click RELOAD ACCOUNT. \n\nI don't know how long this method will last. USE THIS OPTION AT YOUR OWN RISK.",
    "default_dir": "default dir",
    "speed_lower": "speed",
    "status_lower": "status",

    # --- Section B: new ui.* keys
    "ui.error_title": "Error",
    "ui.warning_title": "Warning",
    "ui.elc_error_title": "ELC ERROR",
    "ui.dlc_error_title": "DLC ERROR",
    "ui.confirm.exit_question": "EXIT?",
    "ui.confirm.folder_cache.message":
        "Do you want to use FOLDER CACHED VERSION?\n\n(It could speed up the loading of very large folders)",
    "ui.confirm.folder_cache.message_with_id":
        "Do you want to use FOLDER [{0}] CACHED VERSION?\n\n(It could speed up the loading of very large folders)",
    "ui.confirm.folder_cache.title": "FOLDER CACHE",
    "ui.confirm.clear_debug_log.message":
        "Clear the DEBUG LOG buffer? Existing entries will be lost from the tab.",
    "ui.confirm.clear_debug_log.title": "Clear debug log",
    "ui.err.save_log.message": "Could not save log: {0}",
    "ui.err.save_log.title": "Save failed",
    "ui.err.export_failed.message": "Export failed: {0}",
    "ui.err.export_failed.title": "Export accounts failed",
    "ui.input.megacrypter_password": "Enter password for MegaCrypter link:",
    "ui.dynamic.restart_countdown": "Restart ({0} secs...)",
    "ui.dynamic.transferences_remaining":
        "Transferences remaining: {0}   ({1} workers)",
    "ui.dynamic.downloads_summary": "↓ Downloads: {0}   ({1} workers)",
    "ui.dynamic.uploads_summary": "↑ Uploads: {0}   ({1} workers)",
    "ui.dynamic.force_exit_timer": "Elapsed: {0}s   |   Force-exit in {1}s",
    "ui.dynamic.drained_percent": "{0}% drained",
    "ui.dynamic.checking_account_progress":
        "Checking your MEGA accounts, please wait... {0} ({1}/{2})",
    "ui.dynamic.retry_file_unavailable":
        "File temporarily unavailable! (Retrying in {0} secs...)",
    "ui.dynamic.retry_api_error":
        "Mega/MC APIException error {0} (Retrying in {1} secs...)",
    "ui.export.success": "Exported {0} account(s) to {1}",
    "ui.tooltip.clear_debug_log": "Clear the DEBUG LOG tab.",
    "ui.tooltip.copy_debug_log": "Copy the entire DEBUG LOG buffer to the clipboard.",
    "ui.tooltip.save_debug_log":
        "Write the current DEBUG LOG buffer to a file on disk.",
    "ui.debuglog.clear_button": "Clear",
    "ui.debuglog.copy_button": "Copy all",
    "ui.debuglog.save_button": "Save to file...",
    "ui.debuglog.save_dialog_title": "Save DEBUG LOG to file",
    "ui.filechooser.add_files": "Add files",
    "ui.filechooser.add_directory": "Add directory",
    "ui.filechooser.select_file_part": "Select any part of the original file",
    "ui.filechooser.select_files": "Select file/s",
    "ui.filechooser.download_folder": "Download folder",
    "ui.filechooser.select_dlc": "Select DLC container",
    "ui.filechooser.temp_chunks_dir": "Temporary chunks directory",
    "ui.filechooser.save_as": "Save as",
    "upload_aborted_selected_mega_account_unavailable":
        "Upload aborted: the selected MEGA account is not available.",
}

# ---------------------------------------------------------------------------
# Translations. Each map below should be COMPLETE: every key in EN that has a
# translation here will be appended to the corresponding locale bundle.
# Missing keys cleanly fall back to the English bundle at runtime.
# ---------------------------------------------------------------------------

DE = {
    "warning_caps": "WARNUNG",
    "current_download_chunks_reset": "AKTUELLE DOWNLOAD-CHUNKS ZURÜCKGESETZT!",
    "error_doing_drag_and_drop_with_this_file":
        "FEHLER BEIM DRAG AND DROP MIT DIESER DATEI (verwende die Schaltfläche)",
    "error_checking_account_quota_no_excl": "FEHLER beim Überprüfen des Kontingents",
    "copy_email": "Email kopieren",
    "file_s_successfully_splitted": "Datei(en) erfolgreich aufgeteilt!",
    "mega_folder_temporarily_unavailable": "MEGA-ORDNER VORÜBERGEHEND NICHT VERFÜGBAR!",
    "mega_folder_blocked_deleted": "MEGA-ORDNER GESPERRT/GELÖSCHT",
    "mega_folder_link_error": "FEHLER IM MEGA-ORDNERLINK!",
    "could_not_import_previous_version_settings":
        "Einstellungen der vorherigen Version konnten nicht importiert werden.",
    "import_failed": "Import fehlgeschlagen",
    "file_grabber": "Datei-Grabber",
    "accounts_successfully_reset": "Konten erfolgreich zurückgesetzt!",
    "accounts_reset": "Konten zurücksetzen",
    "there_are_no_accounts_to_export": "Keine Konten zum Exportieren vorhanden.",
    "export_accounts": "Konten exportieren",
    "exported_file_plain_text_warning":
        "Die exportierte Datei enthält deine Zugangsdaten im KLARTEXT. Jeder mit Zugriff auf die Datei kann deine Konten verwenden.\n\nFortfahren?",
    "file_already_exists_overwrite": "Datei existiert bereits. Überschreiben?",
    "using_this_option_corrupt_uploads":
        "Diese Option kann deine Uploads unwiderruflich beschädigen.\n\nNUTZUNG AUF EIGENES RISIKO",
    "txt_file_format": "TXT-DATEIFORMAT",

    "zero_for_permanent_ban": "(0 für dauerhaftes Bannen)",
    "if_proxies_sorted_check_sequential":
        "(Falls du eine Proxyliste vom besten zum schlechtesten sortiert hast, aktiviere sequenziell)",
    "lower_values_speed_up_proxies":
        "(Niedrigere Werte können das Finden funktionierender Proxies beschleunigen, könnten aber langsame Proxies bannen)",
    "useful_to_avoid_slow_proxies":
        "(Nützlich, um nicht in langsamen Proxies hängenzubleiben)",
    "two_fa_code": "2FA-CODE",
    "copy_all_download_links": "ALLE DOWNLOAD-LINKS KOPIEREN",
    "copy_all_upload_links": "ALLE UPLOAD-LINKS KOPIEREN",
    "create_upload_folder_public_link": "ÖFFENTLICHEN UPLOAD-ORDNER-LINK ERSTELLEN",
    "check_version": "Version prüfen",
    "create_upload_thumbnails": "Bild-/Video-Thumbnails erstellen und hochladen",
    "create_upload_logs": "Upload-Logs erstellen",
    "dark_mode": "DUNKLER MODUS",
    "force_all_current_chunks_reset": "ALLE AKTUELLEN CHUNKS ZURÜCKSETZEN ERZWINGEN",
    "force_smart_proxy": "SMART-PROXY ERZWINGEN",
    "file_merger": "Datei-Zusammenführer",
    "file_splitter": "Datei-Splitter",
    "folder_link": "Ordner-Link",
    "forces_smart_proxy_use":
        "Erzwingt die Verwendung des Smart-Proxys auch wenn direkte Bandbreite verfügbar ist (nützlich zum Testen von Proxies)",
    "host_colon": "Host:",
    "master_password_setup": "Master-Passwort einrichten",
    "master_password_unlock": "Master-Passwort entsperren",
    "note1_enable_mitigate_bandwidth":
        "Hinweis1: aktivieren, um Bandbreitenlimit zu mildern. (Multislot ist erforderlich)",
    "note_slots_consume_resources":
        "Hinweis: Slots verbrauchen Ressourcen, also nutze sie mäßig.",
    "proxy_error_ban_time": "Proxy-Fehler-Bannzeit (Sekunden):",
    "proxy_list_refresh": "Proxy-Liste aktualisieren (Minuten):",
    "proxy_selection_order": "Proxy-Auswahlreihenfolge:",
    "proxy_timeout": "Proxy-Timeout (Sekunden):",
    "random_upper": "ZUFÄLLIG",
    "reset_slot_proxy_after_chunk":
        "Slot-Proxy nach erfolgreichem Chunk-Download zurücksetzen",
    "sequential_upper": "SEQUENZIELL",
    "save_debug_info_to_file": "Debug-Info in Datei speichern ->",
    "speed_upper": "Geschwindigkeit",
    "streamer": "Streamer",
    "this_option_not_recommended_upload_folder":
        "DIESE OPTION WIRD NICHT EMPFOHLEN. Sie führt dazu, dass der MegaBasterd-Upload-Ordner in deinem Konto als NICHT ENTSCHLÜSSELBAR erscheint.\n\nZum Zeitpunkt dieser Notiz gibt es eine Methode zur BEHEBUNG:\n\n1) Verschiebe den ersten Upload-Unterordner in den ROOT (CLOUD) Ordner deines Kontos.\n\n2) Gehe zu den Kontoeinstellungen und klicke RELOAD ACCOUNT.\n\nIch weiß nicht, wie lange diese Methode noch funktioniert. NUTZUNG AUF EIGENES RISIKO.",
    "default_dir": "Standardverzeichnis",
    "speed_lower": "geschwindigkeit",
    "status_lower": "status",

    "ui.error_title": "Fehler",
    "ui.warning_title": "Warnung",
    "ui.elc_error_title": "ELC-FEHLER",
    "ui.dlc_error_title": "DLC-FEHLER",
    "ui.confirm.exit_question": "BEENDEN?",
    "ui.confirm.folder_cache.message":
        "Möchtest du die ZWISCHENGESPEICHERTE VERSION des Ordners verwenden?\n\n(Kann das Laden sehr großer Ordner beschleunigen)",
    "ui.confirm.folder_cache.message_with_id":
        "Möchtest du die ZWISCHENGESPEICHERTE VERSION des Ordners [{0}] verwenden?\n\n(Kann das Laden sehr großer Ordner beschleunigen)",
    "ui.confirm.folder_cache.title": "ORDNER-CACHE",
    "ui.confirm.clear_debug_log.message":
        "DEBUG-LOG-Puffer leeren? Vorhandene Einträge gehen aus dem Tab verloren.",
    "ui.confirm.clear_debug_log.title": "Debug-Log leeren",
    "ui.err.save_log.message": "Log konnte nicht gespeichert werden: {0}",
    "ui.err.save_log.title": "Speichern fehlgeschlagen",
    "ui.err.export_failed.message": "Export fehlgeschlagen: {0}",
    "ui.err.export_failed.title": "Export der Konten fehlgeschlagen",
    "ui.input.megacrypter_password": "Passwort für MegaCrypter-Link eingeben:",
    "ui.dynamic.restart_countdown": "Neustart ({0} Sek...)",
    "ui.dynamic.transferences_remaining":
        "Verbleibende Übertragungen: {0}   ({1} Workers)",
    "ui.dynamic.downloads_summary": "↓ Downloads: {0}   ({1} Workers)",
    "ui.dynamic.uploads_summary": "↑ Uploads: {0}   ({1} Workers)",
    "ui.dynamic.force_exit_timer":
        "Verstrichen: {0}s   |   Erzwungenes Beenden in {1}s",
    "ui.dynamic.drained_percent": "{0}% abgeschlossen",
    "ui.dynamic.checking_account_progress":
        "Deine MEGA-Konten werden überprüft, bitte warten... {0} ({1}/{2})",
    "ui.dynamic.retry_file_unavailable":
        "Datei vorübergehend nicht verfügbar! (Erneuter Versuch in {0} Sek...)",
    "ui.dynamic.retry_api_error":
        "Mega/MC-APIException-Fehler {0} (Erneuter Versuch in {1} Sek...)",
    "ui.export.success": "{0} Konto/Konten exportiert nach {1}",
    "ui.tooltip.clear_debug_log": "Den DEBUG-LOG-Tab leeren.",
    "ui.tooltip.copy_debug_log":
        "Den gesamten DEBUG-LOG-Puffer in die Zwischenablage kopieren.",
    "ui.tooltip.save_debug_log":
        "Den aktuellen DEBUG-LOG-Puffer in eine Datei auf der Festplatte schreiben.",
    "ui.debuglog.clear_button": "Leeren",
    "ui.debuglog.copy_button": "Alles kopieren",
    "ui.debuglog.save_button": "In Datei speichern...",
    "ui.debuglog.save_dialog_title": "DEBUG LOG in Datei speichern",
    "ui.filechooser.add_files": "Dateien hinzufügen",
    "ui.filechooser.add_directory": "Verzeichnis hinzufügen",
    "ui.filechooser.select_file_part": "Beliebigen Teil der Originaldatei auswählen",
    "ui.filechooser.select_files": "Datei(en) auswählen",
    "ui.filechooser.download_folder": "Download-Ordner",
    "ui.filechooser.select_dlc": "DLC-Container auswählen",
    "ui.filechooser.temp_chunks_dir": "Temporäres Chunks-Verzeichnis",
    "ui.filechooser.save_as": "Speichern unter",
}

IT = {
    "warning_caps": "ATTENZIONE",
    "current_download_chunks_reset": "CHUNK DEL DOWNLOAD ATTUALE RESETTATI!",
    "error_doing_drag_and_drop_with_this_file":
        "ERRORE NEL TRASCINAMENTO DI QUESTO FILE (usa il metodo del pulsante)",
    "error_checking_account_quota_no_excl": "ERRORE durante il controllo della quota dell'account",
    "copy_email": "Copia email",
    "file_s_successfully_splitted": "File divisi correttamente!",
    "mega_folder_temporarily_unavailable": "CARTELLA MEGA TEMPORANEAMENTE NON DISPONIBILE!",
    "mega_folder_blocked_deleted": "CARTELLA MEGA BLOCCATA/ELIMINATA",
    "mega_folder_link_error": "ERRORE NEL LINK DELLA CARTELLA MEGA!",
    "could_not_import_previous_version_settings":
        "Impossibile importare le impostazioni della versione precedente.",
    "import_failed": "Importazione fallita",
    "file_grabber": "File Grabber",
    "accounts_successfully_reset": "Account reimpostati correttamente!",
    "accounts_reset": "Reimposta account",
    "there_are_no_accounts_to_export": "Non ci sono account da esportare.",
    "export_accounts": "Esporta account",
    "exported_file_plain_text_warning":
        "Il file esportato conterrà le tue credenziali in TESTO IN CHIARO. Chiunque abbia accesso al file può usare i tuoi account.\n\nContinuare?",
    "file_already_exists_overwrite": "Il file esiste già. Sovrascrivere?",
    "using_this_option_corrupt_uploads":
        "L'uso di questa opzione può corrompere irreversibilmente i tuoi upload.\n\nUSALA A TUO RISCHIO",
    "txt_file_format": "FORMATO FILE TXT",

    "zero_for_permanent_ban": "(0 per ban permanente)",
    "if_proxies_sorted_check_sequential":
        "(Se hai una lista di proxy ordinati dal migliore al peggiore, seleziona sequenziale)",
    "lower_values_speed_up_proxies":
        "(Valori più bassi possono accelerare la ricerca di proxy funzionanti ma potrebbero bannare proxy lenti)",
    "useful_to_avoid_slow_proxies": "(Utile per evitare di restare bloccati su proxy lenti)",
    "two_fa_code": "CODICE 2FA",
    "copy_all_download_links": "COPIA TUTTI I LINK DI DOWNLOAD",
    "copy_all_upload_links": "COPIA TUTTI I LINK DI UPLOAD",
    "create_upload_folder_public_link": "CREA LINK PUBBLICO DELLA CARTELLA DI UPLOAD",
    "check_version": "Controlla versione",
    "create_upload_thumbnails": "Crea e carica miniature immagine/video",
    "create_upload_logs": "Crea log di upload",
    "dark_mode": "MODALITÀ SCURA",
    "force_all_current_chunks_reset": "FORZA RESET DI TUTTI I CHUNK CORRENTI",
    "force_smart_proxy": "FORZA SMART PROXY",
    "file_merger": "File Merger",
    "file_splitter": "File Splitter",
    "folder_link": "FolderLink",
    "forces_smart_proxy_use":
        "Forza l'uso dello smart proxy anche se hai ancora banda diretta disponibile (utile per testare i proxy)",
    "host_colon": "Host:",
    "master_password_setup": "Configurazione master password",
    "master_password_unlock": "Sblocco master password",
    "note1_enable_mitigate_bandwidth":
        "Nota1: attivare per mitigare il limite di banda. (Richiede multislot)",
    "note_slots_consume_resources":
        "Nota: gli slot consumano risorse, usali con moderazione.",
    "proxy_error_ban_time": "Tempo ban proxy in errore (secondi):",
    "proxy_list_refresh": "Aggiornamento lista proxy (minuti):",
    "proxy_selection_order": "Ordine di selezione proxy:",
    "proxy_timeout": "Timeout proxy (secondi):",
    "random_upper": "CASUALE",
    "reset_slot_proxy_after_chunk":
        "Reimposta il proxy dello slot dopo il download di un chunk",
    "sequential_upper": "SEQUENZIALE",
    "save_debug_info_to_file": "Salva info di debug su file ->",
    "speed_upper": "Velocità",
    "streamer": "Streamer",
    "this_option_not_recommended_upload_folder":
        "QUESTA OPZIONE NON È CONSIGLIATA. Usandola, la cartella caricata da MegaBasterd apparirà nel tuo account come NON DECIFRABILE.\n\nAl momento di scrivere questo testo, esiste un metodo per RISOLVERE:\n\n1) Sposta la prima sottocartella di upload nella cartella ROOT (CLOUD) del tuo account.\n\n2) Vai nelle impostazioni dell'account e clicca RELOAD ACCOUNT.\n\nNon so quanto durerà questo metodo. USALA A TUO RISCHIO.",
    "default_dir": "directory predefinita",
    "speed_lower": "velocità",
    "status_lower": "stato",

    "ui.error_title": "Errore",
    "ui.warning_title": "Attenzione",
    "ui.elc_error_title": "ERRORE ELC",
    "ui.dlc_error_title": "ERRORE DLC",
    "ui.confirm.exit_question": "USCIRE?",
    "ui.confirm.folder_cache.message":
        "Vuoi usare la VERSIONE IN CACHE della cartella?\n\n(Può velocizzare il caricamento di cartelle molto grandi)",
    "ui.confirm.folder_cache.message_with_id":
        "Vuoi usare la VERSIONE IN CACHE della cartella [{0}]?\n\n(Può velocizzare il caricamento di cartelle molto grandi)",
    "ui.confirm.folder_cache.title": "CACHE CARTELLA",
    "ui.confirm.clear_debug_log.message":
        "Svuotare il buffer del DEBUG LOG? Le voci esistenti andranno perse dalla scheda.",
    "ui.confirm.clear_debug_log.title": "Svuota debug log",
    "ui.err.save_log.message": "Impossibile salvare il log: {0}",
    "ui.err.save_log.title": "Salvataggio fallito",
    "ui.err.export_failed.message": "Esportazione fallita: {0}",
    "ui.err.export_failed.title": "Esportazione account fallita",
    "ui.input.megacrypter_password": "Inserisci la password del link MegaCrypter:",
    "ui.dynamic.restart_countdown": "Riavvia ({0} sec...)",
    "ui.dynamic.transferences_remaining":
        "Trasferimenti rimanenti: {0}   ({1} workers)",
    "ui.dynamic.downloads_summary": "↓ Download: {0}   ({1} workers)",
    "ui.dynamic.uploads_summary": "↑ Upload: {0}   ({1} workers)",
    "ui.dynamic.force_exit_timer": "Trascorso: {0}s   |   Uscita forzata tra {1}s",
    "ui.dynamic.drained_percent": "{0}% drenato",
    "ui.dynamic.checking_account_progress":
        "Verifico i tuoi account MEGA, attendi... {0} ({1}/{2})",
    "ui.dynamic.retry_file_unavailable":
        "File temporaneamente non disponibile! (Nuovo tentativo in {0} sec...)",
    "ui.dynamic.retry_api_error":
        "Errore APIException Mega/MC: {0} (Nuovo tentativo in {1} sec...)",
    "ui.export.success": "Esportati {0} account in {1}",
    "ui.tooltip.clear_debug_log": "Svuota la scheda DEBUG LOG.",
    "ui.tooltip.copy_debug_log":
        "Copia l'intero buffer del DEBUG LOG negli appunti.",
    "ui.tooltip.save_debug_log":
        "Salva il buffer corrente del DEBUG LOG in un file su disco.",
    "ui.debuglog.clear_button": "Svuota",
    "ui.debuglog.copy_button": "Copia tutto",
    "ui.debuglog.save_button": "Salva su file...",
    "ui.debuglog.save_dialog_title": "Salva DEBUG LOG su file",
    "ui.filechooser.add_files": "Aggiungi file",
    "ui.filechooser.add_directory": "Aggiungi cartella",
    "ui.filechooser.select_file_part": "Seleziona una parte qualsiasi del file originale",
    "ui.filechooser.select_files": "Seleziona file",
    "ui.filechooser.download_folder": "Cartella di download",
    "ui.filechooser.select_dlc": "Seleziona contenitore DLC",
    "ui.filechooser.temp_chunks_dir": "Directory chunk temporanei",
    "ui.filechooser.save_as": "Salva come",
}

HU = {
    "warning_caps": "FIGYELEM",
    "current_download_chunks_reset": "AKTUÁLIS LETÖLTÉSI CHUNKOK VISSZAÁLLÍTVA!",
    "error_doing_drag_and_drop_with_this_file":
        "HIBA A FÁJL FOGD ÉS VIDD MŰVELETÉNÉL (használd a gomb módszert)",
    "error_checking_account_quota_no_excl": "HIBA a fiók kvótájának ellenőrzésekor",
    "copy_email": "E-mail másolása",
    "file_s_successfully_splitted": "Fájl(ok) sikeresen feldarabolva!",
    "mega_folder_temporarily_unavailable": "A MEGA MAPPA IDEIGLENESEN NEM ELÉRHETŐ!",
    "mega_folder_blocked_deleted": "A MEGA MAPPA LE VAN TILTVA/TÖRÖLVE",
    "mega_folder_link_error": "HIBA A MEGA MAPPA LINKJÉBEN!",
    "could_not_import_previous_version_settings":
        "Nem sikerült importálni a korábbi verzió beállításait.",
    "import_failed": "Importálás sikertelen",
    "file_grabber": "File Grabber",
    "accounts_successfully_reset": "Fiókok sikeresen visszaállítva!",
    "accounts_reset": "Fiókok visszaállítása",
    "there_are_no_accounts_to_export": "Nincs exportálható fiók.",
    "export_accounts": "Fiókok exportálása",
    "exported_file_plain_text_warning":
        "Az exportált fájl a hitelesítő adataidat NYÍLT SZÖVEGKÉNT tartalmazza. Bárki, aki hozzáfér a fájlhoz, használhatja a fiókjaidat.\n\nFolytatod?",
    "file_already_exists_overwrite": "A fájl már létezik. Felülírod?",
    "using_this_option_corrupt_uploads":
        "Ennek a beállításnak a használata visszafordíthatatlanul tönkreteheti a feltöltéseidet.\n\nHASZNÁLD SAJÁT FELELŐSSÉGRE",
    "txt_file_format": "TXT FÁJL FORMÁTUM",

    "zero_for_permanent_ban": "(0 a végleges tiltáshoz)",
    "if_proxies_sorted_check_sequential":
        "(Ha legjobbtól legrosszabb felé rendezett proxy listád van, válaszd a szekvenciálisat)",
    "lower_values_speed_up_proxies":
        "(Az alacsonyabb értékek felgyorsíthatják a működő proxyk megtalálását, de letilthatják a lassú proxykat)",
    "useful_to_avoid_slow_proxies": "(Hasznos a lassú proxykba ragadás elkerülésére)",
    "two_fa_code": "2FA KÓD",
    "copy_all_download_links": "MINDEN LETÖLTÉSI LINK MÁSOLÁSA",
    "copy_all_upload_links": "MINDEN FELTÖLTÉSI LINK MÁSOLÁSA",
    "create_upload_folder_public_link": "FELTÖLTÉSI MAPPA NYILVÁNOS LINKJÉNEK LÉTREHOZÁSA",
    "check_version": "Verzió ellenőrzése",
    "create_upload_thumbnails": "Kép/videó miniatűrök létrehozása és feltöltése",
    "create_upload_logs": "Feltöltési logok létrehozása",
    "dark_mode": "SÖTÉT MÓD",
    "force_all_current_chunks_reset": "MINDEN AKTUÁLIS CHUNK VISSZAÁLLÍTÁSÁNAK KIKÉNYSZERÍTÉSE",
    "force_smart_proxy": "OKOS PROXY KIKÉNYSZERÍTÉSE",
    "file_merger": "Fájl egyesítő",
    "file_splitter": "Fájl daraboló",
    "folder_link": "Mappa link",
    "forces_smart_proxy_use":
        "Kikényszeríti az okos proxy használatát akkor is, ha még van közvetlen sávszélesség (hasznos a proxyk teszteléséhez)",
    "host_colon": "Hoszt:",
    "master_password_setup": "Mesterjelszó beállítás",
    "master_password_unlock": "Mesterjelszó feloldás",
    "note1_enable_mitigate_bandwidth":
        "1. megjegyzés: aktiváld a sávszélesség-korlát mérséklése érdekében. (Multislot szükséges)",
    "note_slots_consume_resources":
        "Megjegyzés: a slotok erőforrást fogyasztanak, használd őket mértékkel.",
    "proxy_error_ban_time": "Proxy hiba tiltási idő (másodperc):",
    "proxy_list_refresh": "Proxy lista frissítés (perc):",
    "proxy_selection_order": "Proxy kiválasztási sorrend:",
    "proxy_timeout": "Proxy időkorlát (másodperc):",
    "random_upper": "VÉLETLEN",
    "reset_slot_proxy_after_chunk":
        "Slot proxy visszaállítása egy chunk sikeres letöltése után",
    "sequential_upper": "SZEKVENCIÁLIS",
    "save_debug_info_to_file": "Hibakeresési információ mentése fájlba ->",
    "speed_upper": "Sebesség",
    "streamer": "Streamer",
    "this_option_not_recommended_upload_folder":
        "EZ A BEÁLLÍTÁS NEM AJÁNLOTT. Használatával a MegaBasterd által feltöltött mappa NEM DEKÓDOLHATÓKÉNT jelenik meg a fiókodban.\n\nA szöveg írásakor van egy módszer a HELYREÁLLÍTÁSRA:\n\n1) Mozgasd az első feltöltési almappát a fiókod ROOT (CLOUD) mappájába.\n\n2) Lépj a fiók beállításaiba és kattints a RELOAD ACCOUNT gombra.\n\nNem tudom, meddig fog működni ez a módszer. SAJÁT FELELŐSSÉGRE HASZNÁLD.",
    "default_dir": "alapértelmezett mappa",
    "speed_lower": "sebesség",
    "status_lower": "állapot",

    "ui.error_title": "Hiba",
    "ui.warning_title": "Figyelem",
    "ui.elc_error_title": "ELC HIBA",
    "ui.dlc_error_title": "DLC HIBA",
    "ui.confirm.exit_question": "KILÉPSZ?",
    "ui.confirm.folder_cache.message":
        "Szeretnéd használni a mappa CACHELT VERZIÓJÁT?\n\n(Felgyorsíthatja a nagyon nagy mappák betöltését)",
    "ui.confirm.folder_cache.message_with_id":
        "Szeretnéd használni a [{0}] mappa CACHELT VERZIÓJÁT?\n\n(Felgyorsíthatja a nagyon nagy mappák betöltését)",
    "ui.confirm.folder_cache.title": "MAPPA CACHE",
    "ui.confirm.clear_debug_log.message":
        "Kiüríted a DEBUG LOG puffert? A meglévő bejegyzések elvesznek a fülről.",
    "ui.confirm.clear_debug_log.title": "Debug log ürítése",
    "ui.err.save_log.message": "Nem sikerült menteni a logot: {0}",
    "ui.err.save_log.title": "A mentés sikertelen",
    "ui.err.export_failed.message": "Az exportálás sikertelen: {0}",
    "ui.err.export_failed.title": "Fiókok exportálása sikertelen",
    "ui.input.megacrypter_password": "Add meg a MegaCrypter link jelszavát:",
    "ui.dynamic.restart_countdown": "Újraindítás ({0} mp...)",
    "ui.dynamic.transferences_remaining":
        "Hátralévő átvitelek: {0}   ({1} workers)",
    "ui.dynamic.downloads_summary": "↓ Letöltések: {0}   ({1} workers)",
    "ui.dynamic.uploads_summary": "↑ Feltöltések: {0}   ({1} workers)",
    "ui.dynamic.force_exit_timer":
        "Eltelt: {0}s   |   Kényszerített kilépés: {1}s",
    "ui.dynamic.drained_percent": "{0}% kiürítve",
    "ui.dynamic.checking_account_progress":
        "MEGA fiókok ellenőrzése, kérlek várj... {0} ({1}/{2})",
    "ui.dynamic.retry_file_unavailable":
        "A fájl ideiglenesen nem elérhető! (Újrapróbálkozás {0} mp múlva...)",
    "ui.dynamic.retry_api_error":
        "Mega/MC APIException hiba: {0} (Újrapróbálkozás {1} mp múlva...)",
    "ui.export.success": "{0} fiók exportálva ide: {1}",
    "ui.tooltip.clear_debug_log": "DEBUG LOG fül ürítése.",
    "ui.tooltip.copy_debug_log":
        "A teljes DEBUG LOG puffer másolása a vágólapra.",
    "ui.tooltip.save_debug_log":
        "Az aktuális DEBUG LOG puffer mentése lemezfájlba.",
    "ui.debuglog.clear_button": "Ürítés",
    "ui.debuglog.copy_button": "Összes másolása",
    "ui.debuglog.save_button": "Mentés fájlba...",
    "ui.debuglog.save_dialog_title": "DEBUG LOG mentése fájlba",
    "ui.filechooser.add_files": "Fájlok hozzáadása",
    "ui.filechooser.add_directory": "Mappa hozzáadása",
    "ui.filechooser.select_file_part": "Válassz egy tetszőleges részt az eredeti fájlból",
    "ui.filechooser.select_files": "Fájl(ok) kiválasztása",
    "ui.filechooser.download_folder": "Letöltési mappa",
    "ui.filechooser.select_dlc": "DLC tároló kiválasztása",
    "ui.filechooser.temp_chunks_dir": "Ideiglenes chunk könyvtár",
    "ui.filechooser.save_as": "Mentés másként",
}

TR = {
    "warning_caps": "UYARI",
    "current_download_chunks_reset": "GEÇERLİ İNDİRME PARÇALARI SIFIRLANDI!",
    "error_doing_drag_and_drop_with_this_file":
        "BU DOSYAYI SÜRÜKLE BIRAK YAPARKEN HATA (düğme yöntemini kullanın)",
    "error_checking_account_quota_no_excl": "HESAP kotası kontrol edilirken HATA",
    "copy_email": "E-postayı kopyala",
    "file_s_successfully_splitted": "Dosya(lar) başarıyla bölündü!",
    "mega_folder_temporarily_unavailable": "MEGA KLASÖRÜ GEÇİCİ OLARAK KULLANILAMIYOR!",
    "mega_folder_blocked_deleted": "MEGA KLASÖRÜ ENGELLENDİ/SİLİNDİ",
    "mega_folder_link_error": "MEGA KLASÖR BAĞLANTI HATASI!",
    "could_not_import_previous_version_settings":
        "Önceki sürümün ayarları içe aktarılamadı.",
    "import_failed": "İçe aktarma başarısız",
    "file_grabber": "Dosya Yakalayıcı",
    "accounts_successfully_reset": "Hesaplar başarıyla sıfırlandı!",
    "accounts_reset": "Hesapları sıfırla",
    "there_are_no_accounts_to_export": "Dışa aktarılacak hesap yok.",
    "export_accounts": "Hesapları dışa aktar",
    "exported_file_plain_text_warning":
        "Dışa aktarılan dosya kimlik bilgilerinizi DÜZ METİN olarak içerecek. Dosyaya erişimi olan herkes hesaplarınızı kullanabilir.\n\nDevam edilsin mi?",
    "file_already_exists_overwrite": "Dosya zaten var. Üzerine yazılsın mı?",
    "using_this_option_corrupt_uploads":
        "Bu seçeneği kullanmak yüklemelerinizi geri dönüşü olmayan şekilde bozabilir.\n\nKENDİ SORUMLULUĞUNUZDA KULLANIN",
    "txt_file_format": "TXT DOSYA BİÇİMİ",

    "zero_for_permanent_ban": "(0 kalıcı ban için)",
    "if_proxies_sorted_check_sequential":
        "(En iyiden en kötüye sıralanmış bir proxy listeniz varsa, sıralıyı seçin)",
    "lower_values_speed_up_proxies":
        "(Daha düşük değerler çalışan proxy bulmayı hızlandırabilir ama yavaş proxyleri banlayabilir)",
    "useful_to_avoid_slow_proxies": "(Yavaş proxylerde takılı kalmamak için faydalıdır)",
    "two_fa_code": "2FA KODU",
    "copy_all_download_links": "TÜM İNDİRME BAĞLANTILARINI KOPYALA",
    "copy_all_upload_links": "TÜM YÜKLEME BAĞLANTILARINI KOPYALA",
    "create_upload_folder_public_link": "YÜKLEME KLASÖRÜ İÇİN GENEL BAĞLANTI OLUŞTUR",
    "check_version": "Sürüm kontrol et",
    "create_upload_thumbnails": "Resim/video küçük resimlerini oluştur ve yükle",
    "create_upload_logs": "Yükleme günlükleri oluştur",
    "dark_mode": "KARANLIK MOD",
    "force_all_current_chunks_reset": "TÜM GEÇERLİ PARÇALARIN SIFIRLANMASINI ZORLA",
    "force_smart_proxy": "AKILLI PROXY KULLANMAYI ZORLA",
    "file_merger": "Dosya Birleştirici",
    "file_splitter": "Dosya Bölücü",
    "folder_link": "Klasör Bağlantısı",
    "forces_smart_proxy_use":
        "Doğrudan bant genişliği mevcut olsa bile akıllı proxy kullanımını zorlar (proxyleri test etmek için yararlı)",
    "host_colon": "Sunucu:",
    "master_password_setup": "Ana parola ayarla",
    "master_password_unlock": "Ana parola ile aç",
    "note1_enable_mitigate_bandwidth":
        "Not1: bant genişliği sınırını azaltmak için etkinleştirin. (Multislot gerekli)",
    "note_slots_consume_resources":
        "Not: slotlar kaynak tüketir, ölçülü kullanın.",
    "proxy_error_ban_time": "Proxy hata ban süresi (saniye):",
    "proxy_list_refresh": "Proxy listesi yenileme (dakika):",
    "proxy_selection_order": "Proxy seçim sırası:",
    "proxy_timeout": "Proxy zaman aşımı (saniye):",
    "random_upper": "RASTGELE",
    "reset_slot_proxy_after_chunk":
        "Bir parça başarıyla indirildikten sonra slot proxy'sini sıfırla",
    "sequential_upper": "SIRALI",
    "save_debug_info_to_file": "Hata ayıklama bilgilerini dosyaya kaydet ->",
    "speed_upper": "Hız",
    "streamer": "Yayıncı",
    "this_option_not_recommended_upload_folder":
        "BU SEÇENEK ÖNERİLMEZ. Kullanıldığında MegaBasterd ile yüklenen klasör hesabınızda ÇÖZÜLEMEZ olarak görünür.\n\nBu metnin yazıldığı sırada bir DÜZELTME yöntemi var:\n\n1) İlk yükleme alt klasörünü hesabınızın ROOT (CLOUD) klasörüne taşıyın.\n\n2) Hesap ayarlarına gidin ve RELOAD ACCOUNT'a tıklayın.\n\nBu yöntemin ne kadar süre çalışacağını bilmiyorum. KENDİ SORUMLULUĞUNUZDA KULLANIN.",
    "default_dir": "varsayılan dizin",
    "speed_lower": "hız",
    "status_lower": "durum",

    "ui.error_title": "Hata",
    "ui.warning_title": "Uyarı",
    "ui.elc_error_title": "ELC HATASI",
    "ui.dlc_error_title": "DLC HATASI",
    "ui.confirm.exit_question": "ÇIK?",
    "ui.confirm.folder_cache.message":
        "Klasörün ÖNBELLEKTEKİ SÜRÜMÜNÜ kullanmak ister misiniz?\n\n(Çok büyük klasörlerin yüklenmesini hızlandırabilir)",
    "ui.confirm.folder_cache.message_with_id":
        "[{0}] klasörünün ÖNBELLEKTEKİ SÜRÜMÜNÜ kullanmak ister misiniz?\n\n(Çok büyük klasörlerin yüklenmesini hızlandırabilir)",
    "ui.confirm.folder_cache.title": "KLASÖR ÖNBELLEĞİ",
    "ui.confirm.clear_debug_log.message":
        "DEBUG LOG arabelleğini temizle? Sekmedeki mevcut girdiler kaybolacak.",
    "ui.confirm.clear_debug_log.title": "Debug log temizle",
    "ui.err.save_log.message": "Log kaydedilemedi: {0}",
    "ui.err.save_log.title": "Kaydetme başarısız",
    "ui.err.export_failed.message": "Dışa aktarma başarısız: {0}",
    "ui.err.export_failed.title": "Hesap dışa aktarma başarısız",
    "ui.input.megacrypter_password": "MegaCrypter bağlantısı için parolayı girin:",
    "ui.dynamic.restart_countdown": "Yeniden başlat ({0} sn...)",
    "ui.dynamic.transferences_remaining":
        "Kalan aktarımlar: {0}   ({1} workers)",
    "ui.dynamic.downloads_summary": "↓ İndirmeler: {0}   ({1} workers)",
    "ui.dynamic.uploads_summary": "↑ Yüklemeler: {0}   ({1} workers)",
    "ui.dynamic.force_exit_timer":
        "Geçen süre: {0}s   |   Zorla çıkış: {1}s",
    "ui.dynamic.drained_percent": "%{0} boşaltıldı",
    "ui.dynamic.checking_account_progress":
        "MEGA hesaplarınız kontrol ediliyor, lütfen bekleyin... {0} ({1}/{2})",
    "ui.dynamic.retry_file_unavailable":
        "Dosya geçici olarak kullanılamıyor! ({0} sn içinde tekrar denenecek...)",
    "ui.dynamic.retry_api_error":
        "Mega/MC APIException hatası: {0} ({1} sn içinde tekrar denenecek...)",
    "ui.export.success": "{0} hesap dışa aktarıldı: {1}",
    "ui.tooltip.clear_debug_log": "DEBUG LOG sekmesini temizle.",
    "ui.tooltip.copy_debug_log":
        "Tüm DEBUG LOG arabelleğini panoya kopyala.",
    "ui.tooltip.save_debug_log":
        "Mevcut DEBUG LOG arabelleğini diske bir dosyaya yaz.",
    "ui.debuglog.clear_button": "Temizle",
    "ui.debuglog.copy_button": "Hepsini kopyala",
    "ui.debuglog.save_button": "Dosyaya kaydet...",
    "ui.debuglog.save_dialog_title": "DEBUG LOG dosyaya kaydet",
    "ui.filechooser.add_files": "Dosya ekle",
    "ui.filechooser.add_directory": "Dizin ekle",
    "ui.filechooser.select_file_part": "Orijinal dosyanın herhangi bir parçasını seçin",
    "ui.filechooser.select_files": "Dosya(lar) seç",
    "ui.filechooser.download_folder": "İndirme klasörü",
    "ui.filechooser.select_dlc": "DLC kapsayıcısı seç",
    "ui.filechooser.temp_chunks_dir": "Geçici parçalar dizini",
    "ui.filechooser.save_as": "Farklı kaydet",
}

ZH = {
    "warning_caps": "警告",
    "current_download_chunks_reset": "当前下载分块已重置!",
    "error_doing_drag_and_drop_with_this_file":
        "拖放此文件时出错(请使用按钮方式)",
    "error_checking_account_quota_no_excl": "检查账户配额时出错",
    "copy_email": "复制邮箱",
    "file_s_successfully_splitted": "文件已成功分割!",
    "mega_folder_temporarily_unavailable": "MEGA 文件夹暂时不可用!",
    "mega_folder_blocked_deleted": "MEGA 文件夹已被屏蔽/删除",
    "mega_folder_link_error": "MEGA 文件夹链接错误!",
    "could_not_import_previous_version_settings": "无法导入旧版本的设置。",
    "import_failed": "导入失败",
    "file_grabber": "文件抓取器",
    "accounts_successfully_reset": "账户已成功重置!",
    "accounts_reset": "重置账户",
    "there_are_no_accounts_to_export": "没有可导出的账户。",
    "export_accounts": "导出账户",
    "exported_file_plain_text_warning":
        "导出的文件将以明文形式包含您的凭据。任何能访问该文件的人都可以使用您的账户。\n\n是否继续?",
    "file_already_exists_overwrite": "文件已存在,是否覆盖?",
    "using_this_option_corrupt_uploads":
        "使用此选项可能会不可逆地损坏您的上传。\n\n风险自负",
    "txt_file_format": "TXT 文件格式",

    "zero_for_permanent_ban": "(0 表示永久封禁)",
    "if_proxies_sorted_check_sequential":
        "(如果您的代理列表是从最好到最差排序的,请勾选顺序)",
    "lower_values_speed_up_proxies":
        "(较低的值可加快查找可用代理的速度,但可能会封禁慢速代理)",
    "useful_to_avoid_slow_proxies": "(有助于避免被困在慢速代理上)",
    "two_fa_code": "2FA 验证码",
    "copy_all_download_links": "复制所有下载链接",
    "copy_all_upload_links": "复制所有上传链接",
    "create_upload_folder_public_link": "创建上传文件夹公共链接",
    "check_version": "检查版本",
    "create_upload_thumbnails": "创建并上传图像/视频缩略图",
    "create_upload_logs": "创建上传日志",
    "dark_mode": "暗色模式",
    "force_all_current_chunks_reset": "强制重置所有当前分块",
    "force_smart_proxy": "强制智能代理",
    "file_merger": "文件合并器",
    "file_splitter": "文件分割器",
    "folder_link": "文件夹链接",
    "forces_smart_proxy_use":
        "即使仍有直接带宽可用,也强制使用智能代理(用于测试代理)",
    "host_colon": "主机:",
    "master_password_setup": "主密码设置",
    "master_password_unlock": "主密码解锁",
    "note1_enable_mitigate_bandwidth":
        "注1:启用以减轻带宽限制。(需要多插槽)",
    "note_slots_consume_resources":
        "注意:插槽会消耗资源,请适度使用。",
    "proxy_error_ban_time": "代理错误封禁时间(秒):",
    "proxy_list_refresh": "代理列表刷新(分钟):",
    "proxy_selection_order": "代理选择顺序:",
    "proxy_timeout": "代理超时(秒):",
    "random_upper": "随机",
    "reset_slot_proxy_after_chunk":
        "成功下载分块后重置插槽代理",
    "sequential_upper": "顺序",
    "save_debug_info_to_file": "保存调试信息到文件 ->",
    "speed_upper": "速度",
    "streamer": "流媒体",
    "this_option_not_recommended_upload_folder":
        "不建议使用此选项。使用后,MegaBasterd 上传的文件夹将在您的账户中显示为不可解密。\n\n在撰写本文时,有一种修复方法:\n\n1) 将第一个上传子文件夹移动到账户的 ROOT (CLOUD) 文件夹。\n\n2) 进入账户设置并点击 RELOAD ACCOUNT。\n\n我不知道此方法能持续多久。风险自负。",
    "default_dir": "默认目录",
    "speed_lower": "速度",
    "status_lower": "状态",

    "ui.error_title": "错误",
    "ui.warning_title": "警告",
    "ui.elc_error_title": "ELC 错误",
    "ui.dlc_error_title": "DLC 错误",
    "ui.confirm.exit_question": "退出?",
    "ui.confirm.folder_cache.message":
        "是否使用文件夹的缓存版本?\n\n(可加快非常大文件夹的加载速度)",
    "ui.confirm.folder_cache.message_with_id":
        "是否使用文件夹 [{0}] 的缓存版本?\n\n(可加快非常大文件夹的加载速度)",
    "ui.confirm.folder_cache.title": "文件夹缓存",
    "ui.confirm.clear_debug_log.message":
        "清空 DEBUG LOG 缓冲区?标签页中的现有条目将丢失。",
    "ui.confirm.clear_debug_log.title": "清空调试日志",
    "ui.err.save_log.message": "无法保存日志:{0}",
    "ui.err.save_log.title": "保存失败",
    "ui.err.export_failed.message": "导出失败:{0}",
    "ui.err.export_failed.title": "账户导出失败",
    "ui.input.megacrypter_password": "请输入 MegaCrypter 链接的密码:",
    "ui.dynamic.restart_countdown": "重新启动 ({0} 秒...)",
    "ui.dynamic.transferences_remaining":
        "剩余传输: {0}   ({1} workers)",
    "ui.dynamic.downloads_summary": "↓ 下载: {0}   ({1} workers)",
    "ui.dynamic.uploads_summary": "↑ 上传: {0}   ({1} workers)",
    "ui.dynamic.force_exit_timer":
        "已用: {0}s   |   将在 {1}s 后强制退出",
    "ui.dynamic.drained_percent": "{0}% 已清空",
    "ui.dynamic.checking_account_progress":
        "正在检查您的 MEGA 账户,请稍候... {0} ({1}/{2})",
    "ui.dynamic.retry_file_unavailable":
        "文件暂时不可用! ({0} 秒后重试...)",
    "ui.dynamic.retry_api_error":
        "Mega/MC APIException 错误: {0} ({1} 秒后重试...)",
    "ui.export.success": "已导出 {0} 个账户到 {1}",
    "ui.tooltip.clear_debug_log": "清空 DEBUG LOG 标签页。",
    "ui.tooltip.copy_debug_log":
        "将整个 DEBUG LOG 缓冲区复制到剪贴板。",
    "ui.tooltip.save_debug_log":
        "将当前 DEBUG LOG 缓冲区写入磁盘文件。",
    "ui.debuglog.clear_button": "清空",
    "ui.debuglog.copy_button": "全部复制",
    "ui.debuglog.save_button": "保存到文件...",
    "ui.debuglog.save_dialog_title": "将 DEBUG LOG 保存到文件",
    "ui.filechooser.add_files": "添加文件",
    "ui.filechooser.add_directory": "添加目录",
    "ui.filechooser.select_file_part": "选择原始文件的任意部分",
    "ui.filechooser.select_files": "选择文件",
    "ui.filechooser.download_folder": "下载文件夹",
    "ui.filechooser.select_dlc": "选择 DLC 容器",
    "ui.filechooser.temp_chunks_dir": "临时分块目录",
    "ui.filechooser.save_as": "另存为",
}

VI = {
    "warning_caps": "CẢNH BÁO",
    "current_download_chunks_reset": "ĐÃ ĐẶT LẠI CÁC PHẦN TẢI XUỐNG HIỆN TẠI!",
    "error_doing_drag_and_drop_with_this_file":
        "LỖI KHI KÉO THẢ TỆP NÀY (sử dụng phương thức nút)",
    "error_checking_account_quota_no_excl": "LỖI khi kiểm tra hạn ngạch tài khoản",
    "copy_email": "Sao chép email",
    "file_s_successfully_splitted": "Đã chia tệp thành công!",
    "mega_folder_temporarily_unavailable": "THƯ MỤC MEGA TẠM THỜI KHÔNG KHẢ DỤNG!",
    "mega_folder_blocked_deleted": "THƯ MỤC MEGA BỊ CHẶN/XOÁ",
    "mega_folder_link_error": "LỖI LIÊN KẾT THƯ MỤC MEGA!",
    "could_not_import_previous_version_settings":
        "Không thể nhập cài đặt của phiên bản trước.",
    "import_failed": "Nhập thất bại",
    "file_grabber": "Trình lấy tệp",
    "accounts_successfully_reset": "Đã đặt lại tài khoản thành công!",
    "accounts_reset": "Đặt lại tài khoản",
    "there_are_no_accounts_to_export": "Không có tài khoản nào để xuất.",
    "export_accounts": "Xuất tài khoản",
    "exported_file_plain_text_warning":
        "Tệp xuất ra sẽ chứa thông tin đăng nhập của bạn dưới dạng VĂN BẢN THUẦN. Bất kỳ ai truy cập được tệp đều có thể dùng tài khoản của bạn.\n\nTiếp tục?",
    "file_already_exists_overwrite": "Tệp đã tồn tại. Ghi đè?",
    "using_this_option_corrupt_uploads":
        "Việc sử dụng tuỳ chọn này có thể làm hỏng các tệp tải lên của bạn không thể phục hồi.\n\nSỬ DỤNG VỚI RỦI RO TỰ CHỊU",
    "txt_file_format": "ĐỊNH DẠNG TỆP TXT",

    "zero_for_permanent_ban": "(0 để cấm vĩnh viễn)",
    "if_proxies_sorted_check_sequential":
        "(Nếu bạn có danh sách proxy được sắp xếp từ tốt nhất đến tệ nhất, hãy chọn tuần tự)",
    "lower_values_speed_up_proxies":
        "(Giá trị thấp hơn có thể tăng tốc việc tìm proxy hoạt động nhưng có thể cấm các proxy chậm)",
    "useful_to_avoid_slow_proxies": "(Hữu ích để tránh bị mắc kẹt ở các proxy chậm)",
    "two_fa_code": "MÃ 2FA",
    "copy_all_download_links": "SAO CHÉP TẤT CẢ LIÊN KẾT TẢI XUỐNG",
    "copy_all_upload_links": "SAO CHÉP TẤT CẢ LIÊN KẾT TẢI LÊN",
    "create_upload_folder_public_link": "TẠO LIÊN KẾT CÔNG KHAI CHO THƯ MỤC TẢI LÊN",
    "check_version": "Kiểm tra phiên bản",
    "create_upload_thumbnails": "Tạo và tải lên thumbnail hình ảnh/video",
    "create_upload_logs": "Tạo nhật ký tải lên",
    "dark_mode": "CHẾ ĐỘ TỐI",
    "force_all_current_chunks_reset": "BUỘC ĐẶT LẠI TẤT CẢ CÁC PHẦN HIỆN TẠI",
    "force_smart_proxy": "BUỘC DÙNG SMART PROXY",
    "file_merger": "Trình ghép tệp",
    "file_splitter": "Trình chia tệp",
    "folder_link": "Liên kết thư mục",
    "forces_smart_proxy_use":
        "Buộc dùng smart proxy ngay cả khi vẫn còn băng thông trực tiếp (hữu ích để kiểm tra proxy)",
    "host_colon": "Máy chủ:",
    "master_password_setup": "Thiết lập mật khẩu chính",
    "master_password_unlock": "Mở khoá mật khẩu chính",
    "note1_enable_mitigate_bandwidth":
        "Lưu ý 1: bật để giảm giới hạn băng thông. (Yêu cầu multislot)",
    "note_slots_consume_resources":
        "Lưu ý: các slot tiêu tốn tài nguyên, hãy dùng vừa phải.",
    "proxy_error_ban_time": "Thời gian cấm khi lỗi proxy (giây):",
    "proxy_list_refresh": "Làm mới danh sách proxy (phút):",
    "proxy_selection_order": "Thứ tự chọn proxy:",
    "proxy_timeout": "Thời gian chờ proxy (giây):",
    "random_upper": "NGẪU NHIÊN",
    "reset_slot_proxy_after_chunk":
        "Đặt lại proxy của slot sau khi tải xuống thành công một phần",
    "sequential_upper": "TUẦN TỰ",
    "save_debug_info_to_file": "Lưu thông tin gỡ lỗi vào tệp ->",
    "speed_upper": "Tốc độ",
    "streamer": "Streamer",
    "this_option_not_recommended_upload_folder":
        "TUỲ CHỌN NÀY KHÔNG ĐƯỢC KHUYẾN NGHỊ. Khi dùng, thư mục tải lên của MegaBasterd sẽ hiện trong tài khoản của bạn dưới dạng KHÔNG GIẢI MÃ ĐƯỢC.\n\nTại thời điểm viết văn bản này, có một cách để KHẮC PHỤC:\n\n1) Di chuyển thư mục con tải lên đầu tiên vào thư mục ROOT (CLOUD) của tài khoản.\n\n2) Vào cài đặt tài khoản và bấm RELOAD ACCOUNT.\n\nTôi không biết cách này còn hiệu lực bao lâu. SỬ DỤNG VỚI RỦI RO TỰ CHỊU.",
    "default_dir": "thư mục mặc định",
    "speed_lower": "tốc độ",
    "status_lower": "trạng thái",

    "ui.error_title": "Lỗi",
    "ui.warning_title": "Cảnh báo",
    "ui.elc_error_title": "LỖI ELC",
    "ui.dlc_error_title": "LỖI DLC",
    "ui.confirm.exit_question": "THOÁT?",
    "ui.confirm.folder_cache.message":
        "Bạn có muốn dùng PHIÊN BẢN ĐÃ LƯU TRONG CACHE của thư mục?\n\n(Có thể tăng tốc tải các thư mục rất lớn)",
    "ui.confirm.folder_cache.message_with_id":
        "Bạn có muốn dùng PHIÊN BẢN ĐÃ LƯU TRONG CACHE của thư mục [{0}]?\n\n(Có thể tăng tốc tải các thư mục rất lớn)",
    "ui.confirm.folder_cache.title": "CACHE THƯ MỤC",
    "ui.confirm.clear_debug_log.message":
        "Xoá bộ đệm DEBUG LOG? Các mục hiện có trong tab sẽ mất.",
    "ui.confirm.clear_debug_log.title": "Xoá debug log",
    "ui.err.save_log.message": "Không thể lưu log: {0}",
    "ui.err.save_log.title": "Lưu thất bại",
    "ui.err.export_failed.message": "Xuất thất bại: {0}",
    "ui.err.export_failed.title": "Xuất tài khoản thất bại",
    "ui.input.megacrypter_password": "Nhập mật khẩu cho liên kết MegaCrypter:",
    "ui.dynamic.restart_countdown": "Khởi động lại ({0} giây...)",
    "ui.dynamic.transferences_remaining":
        "Còn lại: {0}   ({1} workers)",
    "ui.dynamic.downloads_summary": "↓ Tải xuống: {0}   ({1} workers)",
    "ui.dynamic.uploads_summary": "↑ Tải lên: {0}   ({1} workers)",
    "ui.dynamic.force_exit_timer":
        "Đã qua: {0}s   |   Buộc thoát sau {1}s",
    "ui.dynamic.drained_percent": "{0}% đã rút",
    "ui.dynamic.checking_account_progress":
        "Đang kiểm tra tài khoản MEGA, vui lòng chờ... {0} ({1}/{2})",
    "ui.dynamic.retry_file_unavailable":
        "Tệp tạm thời không khả dụng! (Thử lại sau {0} giây...)",
    "ui.dynamic.retry_api_error":
        "Lỗi Mega/MC APIException: {0} (Thử lại sau {1} giây...)",
    "ui.export.success": "Đã xuất {0} tài khoản vào {1}",
    "ui.tooltip.clear_debug_log": "Xoá tab DEBUG LOG.",
    "ui.tooltip.copy_debug_log":
        "Sao chép toàn bộ bộ đệm DEBUG LOG vào clipboard.",
    "ui.tooltip.save_debug_log":
        "Ghi bộ đệm DEBUG LOG hiện tại ra một tệp trên đĩa.",
    "ui.debuglog.clear_button": "Xoá",
    "ui.debuglog.copy_button": "Sao chép tất cả",
    "ui.debuglog.save_button": "Lưu vào tệp...",
    "ui.debuglog.save_dialog_title": "Lưu DEBUG LOG vào tệp",
    "ui.filechooser.add_files": "Thêm tệp",
    "ui.filechooser.add_directory": "Thêm thư mục",
    "ui.filechooser.select_file_part": "Chọn bất kỳ phần nào của tệp gốc",
    "ui.filechooser.select_files": "Chọn tệp",
    "ui.filechooser.download_folder": "Thư mục tải xuống",
    "ui.filechooser.select_dlc": "Chọn vùng chứa DLC",
    "ui.filechooser.temp_chunks_dir": "Thư mục các phần tạm",
    "ui.filechooser.save_as": "Lưu thành",
}

ES_FORM_GAPS = {
    "zero_for_permanent_ban": "(0 para baneo permanente)",
    "if_proxies_sorted_check_sequential":
        "(Si tienes una lista de proxies ordenada de mejor a peor, marca secuencial)",
    "lower_values_speed_up_proxies":
        "(Los valores bajos pueden acelerar la búsqueda de proxies funcionales pero podrían banear proxies lentos)",
    "useful_to_avoid_slow_proxies":
        "(Útil para no quedarse atascado en proxies lentos)",
    "two_fa_code": "CÓDIGO 2FA",
    "copy_all_download_links": "COPIAR TODOS LOS ENLACES DE DESCARGA",
    "copy_all_upload_links": "COPIAR TODOS LOS ENLACES DE SUBIDA",
    "create_upload_folder_public_link": "CREAR ENLACE PÚBLICO DE LA CARPETA DE SUBIDA",
    "check_version": "Comprobar versión",
    "create_upload_thumbnails": "Crear y subir miniaturas de imagen/vídeo",
    "create_upload_logs": "Crear logs de subida",
    "dark_mode": "MODO OSCURO",
    "force_all_current_chunks_reset": "FORZAR REINICIO DE TODOS LOS CHUNKS ACTUALES",
    "force_smart_proxy": "FORZAR SMART PROXY",
    "file_merger": "Unir archivos",
    "file_splitter": "Dividir archivos",
    "folder_link": "Enlace de carpeta",
    "forces_smart_proxy_use":
        "Fuerza el uso del smart proxy incluso si aún hay ancho de banda directo disponible (útil para probar proxies)",
    "host_colon": "Host:",
    "master_password_setup": "Configurar contraseña maestra",
    "master_password_unlock": "Desbloquear contraseña maestra",
    "note1_enable_mitigate_bandwidth":
        "Nota1: actívalo para mitigar el límite de ancho de banda. (Se requiere multislot)",
    "note_slots_consume_resources":
        "Nota: los slots consumen recursos, úsalos con moderación.",
    "proxy_error_ban_time": "Tiempo de baneo por error de proxy (segundos):",
    "proxy_list_refresh": "Refresco de la lista de proxies (minutos):",
    "proxy_selection_order": "Orden de selección de proxies:",
    "proxy_timeout": "Timeout de proxy (segundos):",
    "random_upper": "ALEATORIO",
    "reset_slot_proxy_after_chunk":
        "Reiniciar el proxy del slot tras descargar un chunk correctamente",
    "sequential_upper": "SECUENCIAL",
    "save_debug_info_to_file": "Guardar info de depuración en archivo ->",
    "speed_upper": "Velocidad",
    "streamer": "Streamer",
    "this_option_not_recommended_upload_folder":
        "ESTA OPCIÓN NO ESTÁ RECOMENDADA. Al usarla, la carpeta subida con MegaBasterd aparecerá en tu cuenta como NO DESCIFRABLE.\n\nEn el momento de escribir este texto hay un método para REPARARLO:\n\n1) Mueve la primera subcarpeta de subida a la carpeta ROOT (CLOUD) de tu cuenta.\n\n2) Ve a los ajustes de la cuenta y haz clic en RELOAD ACCOUNT.\n\nNo sé cuánto tiempo seguirá funcionando este método. ÚSALO BAJO TU PROPIA RESPONSABILIDAD.",
    "default_dir": "directorio por defecto",
    "speed_lower": "velocidad",
    "status_lower": "estado",
}

LOCALES = [
    ("es", ES_FORM_GAPS, "Spanish (.form gaps)"),
    ("de", DE, "German"),
    ("it", IT, "Italian"),
    ("hu", HU, "Hungarian"),
    ("tr", TR, "Turkish"),
    ("zh", ZH, "Chinese"),
    ("vi", VI, "Vietnamese"),
]


def escape_props_value(s: str) -> str:
    out = []
    for i, c in enumerate(s):
        if c == "\\":
            out.append("\\\\")
        elif c == "\n":
            out.append("\\n")
        elif c == "\r":
            out.append("\\r")
        elif c == "\t":
            out.append("\\t")
        elif c == " " and i == 0:
            out.append("\\ ")
        elif ord(c) < 0x20:
            out.append("\\u%04x" % ord(c))
        else:
            out.append(c)
    return "".join(out)


def existing_keys(path: Path):
    s = set()
    if not path.exists():
        return s
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.lstrip()
        if not line or line.startswith("#") or line.startswith("!"):
            continue
        if "=" not in line:
            continue
        k, _ = line.split("=", 1)
        s.add(k)
    return s


def main() -> int:
    for code, trans, lang_name in LOCALES:
        path = BUNDLE_DIR / f"messages_{code}.properties"
        already = existing_keys(path)
        new_entries = []
        for key, value in trans.items():
            if key in already:
                continue
            en_v = EN.get(key, "")
            new_entries.append((key, value, en_v))

        if not new_entries:
            print(f"  {code}: already up to date ({len(already)} keys)")
            continue

        with path.open("a", encoding="utf-8", newline="\n") as f:
            f.write(
                "\n# "
                "-----------------------------------------------------------------------------\n"
            )
            f.write(
                f"# Seeded {lang_name} translations for keys added by the 8.37 i18n refactor.\n"
            )
            f.write(
                "# AI-assisted; please review on https://github.com/tonikelope/megabasterd/issues/397\n"
            )
            f.write(
                "# "
                "-----------------------------------------------------------------------------\n"
            )
            for key, value, en in new_entries:
                if en:
                    f.write("# EN: " + en.replace("\n", " \\n ") + "\n")
                f.write(f"{key}={escape_props_value(value)}\n")
        print(f"  {code}: appended {len(new_entries)} entries")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
