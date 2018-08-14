/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tonikelope.megabasterd;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author tonikelope
 */
public final class LabelTranslatorSingleton {

    public static LabelTranslatorSingleton getInstance() {

        return LabelTranslatorSingleton.LazyHolder.INSTANCE;
    }

    private final ConcurrentHashMap<String, String> _rosetta;

    private LabelTranslatorSingleton() {

        _rosetta = new ConcurrentHashMap();

        switch (MainPanel.getLanguage()) {

            case "ES":
                Spanish();
        }

    }

    private final static class LazyHolder {

        private static final LabelTranslatorSingleton INSTANCE = new LabelTranslatorSingleton();
    }

    private void Spanish() {

        _rosetta.put("Checking your MEGA accounts, please wait...", "Comprobando tus cuentas de MEGA, por favor espera...");
        _rosetta.put("Check version", "Comprobar versión");
        _rosetta.put("Checking, please wait...", "Comprobando, por favor espera...");
        _rosetta.put("You have the latest version ;)", "Tienes la versión más reciente ;)");
        _rosetta.put("Copy MegaBasterd download URL", "Copiar la URL de descarga de MegaBasterd");
        _rosetta.put("Made with love (and with no warranty) by tonikelope.", "Fabricado con amor (y sin ninguna garantía) por tonikelope.");
        _rosetta.put("Yet another (unofficial) cross-platform MEGA downloader/uploader/streaming suite.", "Ni más ni menos que otra suite multiplataforma (no oficial) de descarga/subida/streaming para MEGA.");
        _rosetta.put("MEGA URL was copied to clipboard!", "¡URL de MEGA copiada en el portapapeles!");
        _rosetta.put("MegaBasterd NEW VERSION is available! -> ", "¡Hay una versión nueva de MegaBasterd disponible! -> ");
        _rosetta.put("Selecting folder...", "Seleccionando carpeta...");
        _rosetta.put("MERGE FILE", "JUNTAR ARCHIVO");
        _rosetta.put("Delete parts after merge", "Eliminar partes después de juntar");
        _rosetta.put("Remove selected", "Eliminar seleccionados");
        _rosetta.put("SPLIT FILE", "PARTIR ARCHIVO");
        _rosetta.put("Encrypt on disk sensitive information", "Cifrar en disco datos sensibles");
        _rosetta.put("Use MEGA accounts for download/stream", "Utilizar cuentas de MEGA para descargar/subir");
        _rosetta.put("Please wait...", "Por favor espera...");
        _rosetta.put("CANCEL RETRY", "CANCELAR REINTENTO");
        _rosetta.put("SPLITTING FILE...", "PARTIENDO ARCHIVO...");
        _rosetta.put("Open folder", "Abrir carpeta");
        _rosetta.put("RESUME DOWNLOAD", "CONTINUAR DESCARGA");
        _rosetta.put("Close", "Cerrar");
        _rosetta.put("Add files", "Agregar archivos");
        _rosetta.put("IMPORT SETTINGS", "IMPORTAR AJUSTES");
        _rosetta.put("RESET MEGABASTERD", "RESETEAR MEGABASTERD");
        _rosetta.put("RESET ACCOUNTS", "RESETEAR CUENTAS");
        _rosetta.put("Verify file integrity (when download is finished)", "Verificar integridad del archivo (al terminar la descarga)");
        _rosetta.put("Let's dance, baby", "¡Vamos!");
        _rosetta.put("Unlock accounts", "Desbloquear cuentas");
        _rosetta.put("Use MegaCrypter reverse mode", "Utilizar modo inverso de MegaCrypter");
        _rosetta.put("Select (any) file part", "Seleccionar (alguna) parte del archivo");
        _rosetta.put("Add folder", "Añadir una carpeta");
        _rosetta.put("Adding files, please wait...", "Añadiendo archivos, por favor espera...");
        _rosetta.put("Restart", "Reiniciar");
        _rosetta.put("Use SmartProxy", "Utilizar SmartProxy");
        _rosetta.put("PAUSE DOWNLOAD", "PAUSAR DESCARGA");
        _rosetta.put("Change it", "Cambiar");
        _rosetta.put("EXPORT SETTINGS", "EXPORTAR AJUSTES");
        _rosetta.put("Change output folder", "Cambiar carpeta destino");
        _rosetta.put("Add account", "Añadir cuenta");
        _rosetta.put("Select file", "Seleccionar archivo");
        _rosetta.put("Opening file...", "Abriendo archivo...");
        _rosetta.put("CANCEL DOWNLOAD", "CANCELAR DESCARGA");
        _rosetta.put("REMOVE ALL EXCEPT THIS", "QUITAR TODO EXCEPTO ESTO");
        _rosetta.put("Loading DLC, please wait...", "Cargando DLC, por favor espera...");
        _rosetta.put("Changing output folder...", "Cambiando carpeta destino...");
        _rosetta.put("RESUME UPLOAD", "CONTINUAR SUBIDA");
        _rosetta.put("Limit upload speed", "Limitar velocidad de subida");
        _rosetta.put("CANCEL", "CANCELAR");
        _rosetta.put("Use multi slot download mode", "Utilizar descarga multi slot");
        _rosetta.put("Selecting file...", "Seleccionando archivo...");
        _rosetta.put("Copy folder link", "Copiar enlace de carpeta");
        _rosetta.put("Limit download speed", "Limitar velocidad de descarga");
        _rosetta.put("REMOVE THIS", "QUITAR ESTO");
        _rosetta.put("Copy file link", "Copiar enlace de archivo");
        _rosetta.put("Load DLC container", "Cargar contenedor DLC");
        _rosetta.put("Adding folder, please wait...", "Añadiendo carpeta, por favor espera...");
        _rosetta.put("PAUSE UPLOAD", "PAUSAR SUBIDA");
        _rosetta.put("CANCEL CHECK", "CANCELAR VERIFICACIÓN");
        _rosetta.put("Keep temp file", "Conservar archivo temporal");
        _rosetta.put("Use HTTP(S) PROXY", "Utilizar PROXY HTTP(S)");
        _rosetta.put("MERGING FILE...", "JUNTANDO ARCHIVO...");
        _rosetta.put("Checking MEGA account...", "Comprobando cuenta de MEGA...");
        _rosetta.put("Remove selected", "Quitar seleccionado");
        _rosetta.put("SAVE", "GUARDAR");
        _rosetta.put("New download", "Nueva descarga");
        _rosetta.put("New upload", "Nueva subida");
        _rosetta.put("New stream", "Nuevo stream");
        _rosetta.put("Split file", "Partir un archivo");
        _rosetta.put("Merge file", "Juntar las partes de un archivo");
        _rosetta.put("Remove all no running downloads", "Cancelar las descargas en espera");
        _rosetta.put("Remove all no running uploads", "Cancelar las subidas en espera");
        _rosetta.put("Edit", "Edición");
        _rosetta.put("Settings", "Ajustes");
        _rosetta.put("Help", "Ayuda");
        _rosetta.put("About", "Acerca de");
        _rosetta.put("Remember for this session", "Recordar durante la sesión");
        _rosetta.put("Restore folder data", "Restaurar contenido de la carpeta");
        _rosetta.put("Restoring data, please wait...", "Restaurando datos, por favor espera...");
        _rosetta.put("File", "Archivo");
        _rosetta.put("Hide to tray", "Ocultar en la bandeja");
        _rosetta.put("Close all OK finished", "Cerrar las que finalizaron OK");
        _rosetta.put("Exit", "Salir");
        _rosetta.put("Download folder:", "Carpeta para las descargas:");
        _rosetta.put("Default slots per file:", "Slots por archivo por defecto:");
        _rosetta.put("Note: if you want to download without using a MEGA PREMIUM account you SHOULD enable it. (Slots consume RAM, so use them moderately).", "Nota: si quieres descargar sin utilizar una cuenta de MEGA PREMIUM es recomendable activarlo. (Los slots consumen RAM, así que úsalos con moderación).");
        _rosetta.put("Max speed (KB/s):", "Velocidad máxima (KB/s):");
        _rosetta.put("Default account:", "Cuenta por defecto:");
        _rosetta.put("TCP Port:", "Puerto TCP:");
        _rosetta.put("Note: you MUST \"OPEN\" this port in your router/firewall.", "Nota: es OBLIGATORIO \"ABRIR\" este puerto en tu router/firewall.");
        _rosetta.put("Note: enable it in order to mitigate bandwidth limit. (Multi slot required).", "Nota: actívalo para mitigar el límite de descarga de MEGA.");
        _rosetta.put("Max parallel downloads:", "Máximas descargas simultáneas:");
        _rosetta.put("Max parallel uploads:", "Máximas subidas simultáneas:");
        _rosetta.put("Note: slots consume RAM, so use them moderately.", "Nota: los slots consumen RAM, así que úsalos con moderación");
        _rosetta.put("Note: you can use a (optional) alias for your email addresses -> bob@supermail.com#bob_mail (don't forget to save after entering your accounts).", "Nota: puedes usar de forma opcional un alias para tus emails -> bob@supermail.com#bob_mail (no te olvides de guardar después de introducir todas tus cuentas).");
        _rosetta.put("Your MEGA accounts:", "Tus cuentas de MEGA:");
        _rosetta.put("Your ELC accounts:", "Tus cuentas ELC:");
        _rosetta.put("Note: restart required.", "Nota: es necesario reiniciar.");
        _rosetta.put("Font ZOOM (%):", "Zoom de la fuente (%):");
        _rosetta.put("Note: MegaBasterd will use this proxy for ALL connections (restart required).", "Nota: MegaBasterd utilizará este proxy para todas las conexiones (es necesario reiniciar).");
        _rosetta.put("Port:", "Puerto:");
        _rosetta.put("Settings successfully saved!", "¡Ajustes guardados correctamente!");
        _rosetta.put("Settings successfully imported!", "¡Ajustes importados correctamente!");
        _rosetta.put("Settings successfully exported!", "¡Ajustes exportados correctamente!");
        _rosetta.put("Settings successfully reset!", "¡Ajustes reseteados correctamente!");
        _rosetta.put("There were errors with some accounts (email and/or password are/is wrong). Please, check them:\n\n", "Hay errores con algunas cuentas (email y/o contraseña incorrecta). Por favor, revísalas:\n\n");
        _rosetta.put("Settings saved", "Ajustes guardados");
        _rosetta.put("Settings imported", "Ajustes importados");
        _rosetta.put("Settings exported", "Ajustes exportados");
        _rosetta.put("Settings reset", "Ajustes reseteados");
        _rosetta.put("MegaBasterd will restart", "MegaBasterd se reiniciará");
        _rosetta.put("Restart required", "Es necesario reiniciar");
        _rosetta.put("File is not readable!", "¡Archivo no accesible!");
        _rosetta.put("Folder is not readable!", "¡Carpeta no accesible!");
        _rosetta.put("BAD PASSWORD!", "¡CONTRASEÑA INCORRECTA!");
        _rosetta.put("Passwords does not match!", "¡Las contraseñas no coinciden!");
        _rosetta.put("Please, paste a Mega/MegaCrypter/ELC link!", "Por favor, escribe un enlace de Mega/MegaCrypter/ELC");
        _rosetta.put("Yes", "Sí");
        _rosetta.put("Cancel", "Cancelar");
        _rosetta.put("It seems MegaBasterd is streaming video. Do you want to exit?", "Parece que MegaBasterd está retransmitiendo vídeo. ¿Quieres continuar?");
        _rosetta.put("It seems MegaBasterd is provisioning down/uploads.\n\nIf you exit now, unprovisioned down/uploads will be lost.\n\nDo you want to continue?", "Parece que MegaBasterd está provisionando transferencias.\n\nSi sales ahora, todas las transferencias no provisionadas se perderán.\n\n¿Quieres continuar?");
        _rosetta.put("It seems MegaBasterd is just finishing uploading some files.\n\nIF YOU EXIT NOW, THOSE UPLOADS WILL FAIL.\n\nDo you want to continue?", "Parece que MegaBasterd está finalizando unas subidas.\n\nSI SALES AHORA, ESAS SUBIDAS FALLARÁN.\n\n¿Quieres continuar?");
        _rosetta.put("All your current settings and accounts will be deleted after import. (It is recommended to export your current settings before importing). \n\nDo you want to continue?", "Todos tus ajustes y cuentas actuales se perderán después de importar. (Es recomendable guardar tus ajustes actuales antes de importar otros). ¿Quieres continuar?");
        _rosetta.put("Only SAVED settings and accounts will be exported. (If you are unsure, it is better to save your current settings and then export them).\n\nDo you want to continue?", "Sólamente los ajustes GUARDADOS se exportarán. (Si no estás seguro, es mejor que guardes antes tus ajustes y que después vuelvas aquí). ¿Quieres continuar?");
        _rosetta.put("Master password will be reset and all your accounts will be removed. (THIS CAN'T BE UNDONE)\n\nDo you want to continue?", "La contraseña maestra será reseteada y todas tus cuentas serán eliminadas. (ESTO NO SE PUEDE DESHACER).\n\n¿Quieres continuar?");
        _rosetta.put("ALL YOUR SETTINGS, ACCOUNTS AND TRANSFERENCES WILL BE REMOVED. (THIS CAN'T BE UNDONE)\n\nDo you want to continue?", "TODOS TUS AJUSTES, CUENTAS Y TRANSFERENCIAS SERÁN ELIMINADAS. (ESTO NO SE PUEDE DESHACER)\n\n¿Quieres continuar?");
        _rosetta.put("Remove all preprocessing, provisioning and waiting downloads?", "¿Eliminar todas las descargas que no están en ejecución?");
        _rosetta.put("Warning!", "¡Atención!");
        _rosetta.put("Remove all preprocessing, provisioning and waiting uploads?", "¿Eliminar todas las subidas que no están en ejecución?");
        _rosetta.put("Please, enter your master password", "Por favor, ingresa tu contraseña maestra");
        _rosetta.put("WARNING: if you forget this password, you will have to insert all your accounts again.", "AVISO: si olvidas esta contraseña, tendrás que volver a insertar todas tus cuentas.");
        _rosetta.put("New pass:", "Nueva contraseña:");
        _rosetta.put("Confirm new:", "Confirmar nueva contraseña:");
        _rosetta.put("Verifying your password, please wait...", "Verificando tu contraseña, por favor espera...");
        _rosetta.put("Processing your password, please wait...", "Procesando tu contraseña, por favor espera...");
        _rosetta.put("Downloads", "Descargas");
        _rosetta.put("Uploads", "Subidas");
        _rosetta.put("Accounts", "Cuentas");
        _rosetta.put("Advanced", "Avanzado");
        _rosetta.put("Language:", "Idioma:");
        _rosetta.put("English", "Inglés");
        _rosetta.put("Spanish", "Español");
        _rosetta.put("Checking account quota, please wait...", "Comprobando espacio utilizado de la cuenta, por favor espera...");
        _rosetta.put("ERROR checking account quota!", "ERROR al comprobar el espacio utilizado.");
        _rosetta.put("Quota used: ", "Espacio utilizado: ");
        _rosetta.put("If you DO NOT want to upload some folder or file you can REMOVE it (to select several items at the same time use CTRL + LMOUSE).", "Si no quieres subir alguna carpeta o archivo puedes quitarlo de la lista (para seleccionar varios elementos al mismo tiempo usa CTRL + RATÓN_IZQ)");
        _rosetta.put("Upload name:", "Nombre de la subida:");
        _rosetta.put("Account:", "Cuenta:");
        _rosetta.put("Folder link detected!", "¡Enlace de carpeta detectado!");
        _rosetta.put("File already exists!", "¡El archivo ya existe!");
        _rosetta.put("Megabasterd is stopping transferences safely, please wait...", "MegaBasterd está deteniendo las transferencias de forma segura, por favor espera...");
        _rosetta.put("Put your MEGA/MegaCrypter/ELC link/s here (one per line):", "Pon aquí tus enlaces de MEGA/MegaCrypter/ELC (uno por línea):");
        _rosetta.put("Download folder:", "Carpeta de descarga:");
        _rosetta.put("Split size (MBs):", "Tamaño de cada parte (MBs):");
        _rosetta.put("File successfully splitted!", "¡Archivo partido correctamente!");
        _rosetta.put("File successfully merged!", "¡Archivo juntado correctamente!");
        _rosetta.put("File successfuly downloaded!", "¡Archivo descargado correctamente!");
        _rosetta.put("Download paused!", "¡Descarga pausada!");
        _rosetta.put("Downloading file from mega ...", "Descargando archivo de mega ...");
        _rosetta.put("Open folder", "Abrir carpeta");
        _rosetta.put("Copy link", "Copiar enlace");
        _rosetta.put("Close", "Cerrar");
        _rosetta.put("Restart", "Reiniciar");
        _rosetta.put("Downloading file from mega ", "Descargando archivo de mega ");
        _rosetta.put("EXIT NOW", "SALIR AHORA MISMO");
        _rosetta.put("Download CANCELED!", "¡Descarga CANCELADA!");
        _rosetta.put("Upload CANCELED!", "¡Subida CANCELADA!");
        _rosetta.put("Uploading file to mega (", "Subiendo archivo a mega (");
        _rosetta.put("Put your MEGA/MegaCrypter/ELC link here in order to get a streaming link:", "Pon aquí tu enlace de MEGA/MegaCrypter/ELC para conseguir un enlace de streaming:");
        _rosetta.put("Use this account for streaming:", "Usar esta cuenta para streaming:");
        _rosetta.put("Use this account for download:", "Usar esta cuenta para descargar:");
        _rosetta.put("Streaming link was copied to clipboard!\nRemember to keep MegaBasterd running in background while playing content.", "Enlace de streaming copiado al portapapeles!\nRecuerda dejar abierto MegaBasterd mientras se reproduce el contenido.");
        _rosetta.put("CANCEL UPLOAD", "CANCELAR SUBIDA");
        _rosetta.put("Upload paused!", "¡Subida pausada!");
        _rosetta.put("Stopping download safely before exit MegaBasterd, please wait...", "Deteniendo descarga de forma segura antes de salir, por favor espera...");
        _rosetta.put("Stopping upload safely before exit MegaBasterd, please wait...", "Deteniendo subida de forma segura antes de salir, por favor espera...");
        _rosetta.put("Starting download, please wait...", "Empezando descarga, por favor espera...");
        _rosetta.put("Starting upload, please wait...", "Empezando subida, por favor espera...");
        _rosetta.put("Starting download (retrieving MEGA temp link), please wait...", "Empezando descarga (pidiendo enlace temporal de MEGA), por favor espera...");
        _rosetta.put("File exists, resuming download...", "El archivo existe, continuando descarga...");
        _rosetta.put("Truncating temp file...", "Truncando fichero temporal...");
        _rosetta.put("Waiting to check file integrity...", "Esperando para verificar integridad del archivo...");
        _rosetta.put("Checking file integrity, please wait...", "Verificando integridad del archivo...");
        _rosetta.put("Provisioning download, please wait...", "Aprovisionando descarga, por favor espera...");
        _rosetta.put("Waiting to start...", "Esperando para empezar...");
        _rosetta.put("Provisioning upload, please wait...", "Aprovisionando subida, por favor espera...");
        _rosetta.put("Waiting to start (", "Esperando para empezar (");
        _rosetta.put("Creating new MEGA node ... ***DO NOT EXIT MEGABASTERD NOW***", "Creando nuevo nodo de MEGA ... ***NO CIERRES MEGABASTERD EN ESTE MOMENTO***");
        _rosetta.put("File successfully uploaded! (", "¡Archivo subido correctamente! (");
        _rosetta.put("PAUSE ALL", "PAUSAR TODO");
        _rosetta.put("MEGA folder link was copied to clipboard!", "¡Enlace de carpeta de MEGA copiado al portapapeles!");
        _rosetta.put("MEGA file link was copied to clipboard!", "¡Enlace de fichero de MEGA copiado al portapapeles!");
        _rosetta.put("Pausing download ...", "Pausando descarga ...");
        _rosetta.put("Pausing upload ...", "Pausando subida ...");
        _rosetta.put("File successfully downloaded! (Integrity check PASSED)", "¡Archivo descargado correctamente! (Test de integridad CORRECTO)");
        _rosetta.put("File successfully downloaded! (but integrity check CANCELED)", "¡Archivo descargado correctamente! (Test de integridad cancelado)");
        _rosetta.put("UPLOAD FAILED! (Empty completion handle!)", "¡SUBIDA FALLIDA! (Completion handle vacío)");
        _rosetta.put("BAD NEWS :( File is DAMAGED!", "MALAS NOTICIAS :( El archivo está corrupto!");
        _rosetta.put("File temporarily unavailable! (Retrying in ", "¡Archivo temporalmente no disponible! (Reintentando en ");
        _rosetta.put(" secs...)", " segundos...)");
        _rosetta.put(" (Retrying in ", " (Reintentando en ");
        _rosetta.put("Proxy settings", "Ajustes de proxy");
        _rosetta.put("Authentication", "Autenticación");
        _rosetta.put("Upload info", "Información de la subida");
        _rosetta.put("Files", "Archivos");
        _rosetta.put("Username:", "Usuario:");
        _rosetta.put("Password:", "Contraseña:");
        _rosetta.put("Link was copied to clipboard!", "¡Enlace copiado al portapapeles!");
        _rosetta.put("Checking if there are previous downloads, please wait...", "Comprobando si existen descargas previas, por favor espera...");
        _rosetta.put("Checking if there are previous uploads, please wait...", "Comprobando si existen subidas previas, por favor espera...");
        _rosetta.put("Restore window", "Restaurar ventana");
        _rosetta.put("EXIT", "SALIR");
        _rosetta.put("File successfully downloaded!", "¡Archivo descargado correctamente!");
        _rosetta.put("Quota used: ", "Espacio usado: ");
        _rosetta.put("Streaming server: ON (port ", "Servidor de streaming: ON (puerto ");
        _rosetta.put("MC reverse mode: ON (port ", "MC reverse mode: ON (puerto ");

    }

    public String translate(String orig) {

        return _rosetta.containsKey(orig) ? (String) _rosetta.get(orig) : orig;
    }
}
