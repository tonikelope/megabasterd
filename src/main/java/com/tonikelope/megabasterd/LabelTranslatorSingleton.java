/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tonikelope.megabasterd;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public class LabelTranslatorSingleton {

    private static final Logger LOG = Logger.getLogger(LabelTranslatorSingleton.class.getName());

    public static LabelTranslatorSingleton getInstance() {

        return LabelTranslatorSingleton.LazyHolder.INSTANCE;
    }

    private final ConcurrentHashMap<String, String> _rosetta;

    private LabelTranslatorSingleton() {

        _rosetta = new ConcurrentHashMap();

        switch (MainPanel.getLanguage()) {

            case "ES":
                Spanish();
                break;
            case "TC":
                TraditionalChinese();
                break;
        }

    }

    private void Spanish() {

        _addTranslation("Use this proxy list (instead of the one included in MegaBasterd) Format is PROXY:PORT", "Usar esta lista de proxys (en vez de la incluida en MegaBasterd) El formato es PROXY:PUERTO");
        _addTranslation("Waiting for completion handler ... ***DO NOT EXIT MEGABASTERD NOW***", "Esperando manejador de finalización ... ***NO CIERRES MEGABASTERD EN ESTE MOMENTO***");
        _addTranslation("Finishing calculating CBC-MAC code (this could take a while) ... ***DO NOT EXIT MEGABASTERD NOW***", "Terminando de calcular código CBC-MAC (esto podría llevar tiempo) ... ***NO CIERRES MEGABASTERD EN ESTE MOMENTO***");
        _addTranslation("Split content in different uploads", "Separar contenido en diferentes subidas");
        _addTranslation("Merge content in the same upload", "Juntar todo en la misma subida");
        _addTranslation("How do you want to proceed?", "¿Qué quieres hacer?");
        _addTranslation("TOP", "ENCIMA DE TODO");
        _addTranslation("BOTTOM", "DEBAJO DE TODO");
        _addTranslation("Freeze transferences before start", "Congelar transferencias antes de empezar");
        _addTranslation("UNFREEZE WAITING TRANSFERENCES", "DESCONGELAR TRANSFERENCIAS A LA ESPERA");
        _addTranslation("(FROZEN) Waiting to start...", "(CONGELADA) Esperando para empezar...");
        _addTranslation("(FROZEN) Waiting to start (", "(CONGELADA) Esperando para empezar (");
        _addTranslation("There are a lot of files in this folder.\nNot all links will be provisioned at once to avoid saturating MegaBasterd", "Hay muchos archivos en esta carpeta.\nNo se aprovisionarán todos los enlaces de una vez para evitar saturar MegaBasterd");
        _addTranslation("You've tried to login too many times. Wait an hour.", "Has intentado hacer login demasiadas veces. Prueba dentro de una hora.");
        _addTranslation("MEGA LINK ERROR!", "¡ENLACE DE MEGA ERRÓNEO!");
        _addTranslation("Please enter 2FA PIN CODE", "Por favor, introduce el CÓDIGO 2FA");
        _addTranslation("Enable log file", "Activar fichero de log");
        _addTranslation("Font:", "Fuente:");
        _addTranslation("Loading...", "Cargando...");
        _addTranslation("DEFAULT", "POR DEFECTO");
        _addTranslation("ALTERNATIVE", "ALTERNATIVA");
        _addTranslation("Download latest version", "Descargar la última versión");
        _addTranslation("PROVISION FAILED", "ERROR AL APROVISIONAR");
        _addTranslation("Error registering download: file is already downloading.", "Error al registrar la descarga: el archivo ya se está descargando.");
        _addTranslation("FATAL ERROR! ", "¡ERROR FATAL! ");
        _addTranslation("ERROR: FILE NOT FOUND", "ERROR: ARCHIVO NO ENCONTRADO");
        _addTranslation("Mega link is not valid! ", "Enlace de MEGA incorrecto! ");
        _addTranslation("Checking your MEGA accounts, please wait...", "Comprobando tus cuentas de MEGA, por favor espera...");
        _addTranslation("Check for updates", "Comprobar actualización");
        _addTranslation("Checking, please wait...", "Comprobando, por favor espera...");
        _addTranslation("You have the latest version ;)", "Tienes la versión más reciente ;)");
        _addTranslation("Copy MegaBasterd download URL", "Copiar la URL de descarga de MegaBasterd");
        _addTranslation("Made with love (and with no warranty) by tonikelope.", "Fabricado con amor (y sin ninguna garantía) por tonikelope.");
        _addTranslation("Yet another unofficial (and ugly) cross-platform MEGA downloader/uploader/streaming suite.", "Ni más ni menos que otra suite multiplataforma no oficial (y fea) de descarga/subida/streaming para MEGA.");
        _addTranslation("MEGA URL was copied to clipboard!", "¡URL de MEGA copiada en el portapapeles!");
        _addTranslation("MegaBasterd NEW VERSION is available! -> ", "¡Hay una versión nueva de MegaBasterd disponible! -> ");
        _addTranslation("Selecting folder...", "Seleccionando carpeta...");
        _addTranslation("MERGE FILE", "JUNTAR ARCHIVO");
        _addTranslation("Delete parts after merge", "Eliminar partes después de juntar");
        _addTranslation("Remove selected", "Quitar seleccionado");
        _addTranslation("SPLIT FILE", "PARTIR ARCHIVO");
        _addTranslation("Encrypt on disk sensitive information", "Cifrar en disco datos sensibles");
        _addTranslation("Allow using MEGA accounts for download/streaming", "Permitir utilizar cuentas de MEGA para descargar/streaming");
        _addTranslation("Please wait...", "Por favor espera...");
        _addTranslation("CANCEL RETRY", "CANCELAR REINTENTO");
        _addTranslation("SPLITTING FILE...", "PARTIENDO ARCHIVO...");
        _addTranslation("Open folder", "Abrir carpeta");
        _addTranslation("RESUME DOWNLOAD", "CONTINUAR DESCARGA");
        _addTranslation("Close", "Cerrar");
        _addTranslation("Add files", "Agregar archivos");
        _addTranslation("IMPORT SETTINGS", "IMPORTAR AJUSTES");
        _addTranslation("RESET MEGABASTERD", "RESETEAR MEGABASTERD");
        _addTranslation("RESET ACCOUNTS", "RESETEAR CUENTAS");
        _addTranslation("Verify file integrity (when download is finished)", "Verificar integridad del archivo (al terminar la descarga)");
        _addTranslation("Let's dance, baby", "¡Vamos!");
        _addTranslation("Unlock accounts", "Desbloquear cuentas");
        _addTranslation("Use MegaCrypter reverse mode", "Utilizar modo inverso de MegaCrypter");
        _addTranslation("Select (any) file part", "Seleccionar (alguna) parte del archivo");
        _addTranslation("Add folder", "Añadir una carpeta");
        _addTranslation("Adding files, please wait...", "Añadiendo archivos, por favor espera...");
        _addTranslation("Restart", "Reiniciar");
        _addTranslation("Use SmartProxy", "Utilizar SmartProxy");
        _addTranslation("PAUSE DOWNLOAD", "PAUSAR DESCARGA");
        _addTranslation("Change it", "Cambiar");
        _addTranslation("EXPORT SETTINGS", "EXPORTAR AJUSTES");
        _addTranslation("Change output folder", "Cambiar carpeta destino");
        _addTranslation("Add account", "Añadir cuenta");
        _addTranslation("Select file", "Seleccionar archivo");
        _addTranslation("Opening file...", "Abriendo archivo...");
        _addTranslation("CANCEL DOWNLOAD", "CANCELAR DESCARGA");
        _addTranslation("REMOVE ALL EXCEPT THIS", "QUITAR TODO EXCEPTO ESTO");
        _addTranslation("Loading DLC, please wait...", "Cargando DLC, por favor espera...");
        _addTranslation("Changing output folder...", "Cambiando carpeta destino...");
        _addTranslation("RESUME UPLOAD", "CONTINUAR SUBIDA");
        _addTranslation("Limit upload speed", "Limitar velocidad de subida");
        _addTranslation("CANCEL", "CANCELAR");
        _addTranslation("Use multi slot download mode", "Utilizar descarga multi slot");
        _addTranslation("Selecting file...", "Seleccionando archivo...");
        _addTranslation("Copy folder link", "Copiar enlace de carpeta");
        _addTranslation("Limit download speed", "Limitar velocidad de descarga");
        _addTranslation("REMOVE THIS", "QUITAR ESTO");
        _addTranslation("Copy file link", "Copiar enlace de archivo");
        _addTranslation("Load DLC container", "Cargar contenedor DLC");
        _addTranslation("Adding folder, please wait...", "Añadiendo carpeta, por favor espera...");
        _addTranslation("PAUSE UPLOAD", "PAUSAR SUBIDA");
        _addTranslation("CANCEL CHECK", "CANCELAR VERIFICACIÓN");
        _addTranslation("Keep temp file", "Conservar archivo temporal");
        _addTranslation("Use HTTP(S) PROXY", "Utilizar PROXY HTTP(S)");
        _addTranslation("MERGING FILE...", "JUNTANDO ARCHIVO...");
        _addTranslation("Checking MEGA account...", "Comprobando cuenta de MEGA...");
        _addTranslation("SAVE", "GUARDAR");
        _addTranslation("New download", "Nueva descarga");
        _addTranslation("New upload", "Nueva subida");
        _addTranslation("New streaming", "Nuevo streaming");
        _addTranslation("Split file", "Partir un archivo");
        _addTranslation("Merge file", "Juntar las partes de un archivo");
        _addTranslation("Remove all no running downloads", "Cancelar las descargas en espera");
        _addTranslation("Remove all no running uploads", "Cancelar las subidas en espera");
        _addTranslation("Edit", "Edición");
        _addTranslation("Settings", "Ajustes");
        _addTranslation("Help", "Ayuda");
        _addTranslation("About", "Acerca de");
        _addTranslation("Remember for this session", "Recordar durante la sesión");
        _addTranslation("Restore folder data", "Restaurar contenido de la carpeta");
        _addTranslation("Restoring data, please wait...", "Restaurando datos, por favor espera...");
        _addTranslation("File", "Archivo");
        _addTranslation("Hide to tray", "Ocultar en la bandeja");
        _addTranslation("Clear finished", "Cerrar las que finalizaron");
        _addTranslation("Exit", "Salir");
        _addTranslation("Default slots per file:", "Slots por archivo por defecto:");
        _addTranslation("Note: if you want to download without using a MEGA PREMIUM account you SHOULD enable it. (Slots consume RAM, so use them moderately).", "Nota: si quieres descargar sin utilizar una cuenta de MEGA PREMIUM es recomendable activarlo. (Los slots consumen RAM, así que úsalos con moderación).");
        _addTranslation("Max speed (KB/s):", "Velocidad máxima (KB/s):");
        _addTranslation("Default account:", "Cuenta por defecto:");
        _addTranslation("TCP Port:", "Puerto TCP:");
        _addTranslation("Note: you MUST \"OPEN\" this port in your router/firewall.", "Nota: es OBLIGATORIO \"ABRIR\" este puerto en tu router/firewall.");
        _addTranslation("Note: enable it in order to mitigate bandwidth limit. (Multi slot required).", "Nota: actívalo para mitigar el límite de descarga de MEGA. (Se requiere muti slot).");
        _addTranslation("Max parallel downloads:", "Máximas descargas simultáneas:");
        _addTranslation("Max parallel uploads:", "Máximas subidas simultáneas:");
        _addTranslation("Note: slots consume RAM, so use them moderately.", "Nota: los slots consumen RAM, así que úsalos con moderación");
        _addTranslation("Note: you can use a (optional) alias for your email addresses -> bob@supermail.com#bob_mail (don't forget to save after entering your accounts).", "Nota: puedes usar de forma opcional un alias para tus emails -> bob@supermail.com#bob_mail (no te olvides de guardar después de introducir todas tus cuentas).");
        _addTranslation("Your MEGA accounts:", "Tus cuentas de MEGA:");
        _addTranslation("Your ELC accounts:", "Tus cuentas ELC:");
        _addTranslation("Note: restart required.", "Nota: es necesario reiniciar.");
        _addTranslation("Note: restart might be required.", "Nota: podría ser necesario reiniciar.");
        _addTranslation("Font ZOOM (%):", "Zoom de la fuente (%):");
        _addTranslation("Note: MegaBasterd will use this proxy for ALL connections.", "Nota: MegaBasterd utilizará este proxy para todas las conexiones.");
        _addTranslation("Port:", "Puerto:");
        _addTranslation("Settings successfully saved!", "¡Ajustes guardados correctamente!");
        _addTranslation("Settings successfully imported!", "¡Ajustes importados correctamente!");
        _addTranslation("Settings successfully exported!", "¡Ajustes exportados correctamente!");
        _addTranslation("Settings successfully reset!", "¡Ajustes reseteados correctamente!");
        _addTranslation("There were errors with some accounts (email and/or password are/is wrong). Please, check them:\n\n", "Hay errores con algunas cuentas (email y/o contraseña incorrectos). Por favor, revísalas:\n\n");
        _addTranslation("Settings saved", "Ajustes guardados");
        _addTranslation("Settings imported", "Ajustes importados");
        _addTranslation("Settings exported", "Ajustes exportados");
        _addTranslation("Settings reset", "Ajustes reseteados");
        _addTranslation("MegaBasterd will restart", "MegaBasterd se reiniciará");
        _addTranslation("Restart required", "Es necesario reiniciar");
        _addTranslation("File is not readable!", "¡Archivo no accesible!");
        _addTranslation("Folder is not readable!", "¡Carpeta no accesible!");
        _addTranslation("BAD PASSWORD!", "¡CONTRASEÑA INCORRECTA!");
        _addTranslation("Passwords does not match!", "¡Las contraseñas no coinciden!");
        _addTranslation("Please, paste a Mega/MegaCrypter/ELC link!", "Por favor, escribe un enlace de Mega/MegaCrypter/ELC");
        _addTranslation("Yes", "Sí");
        _addTranslation("Cancel", "Cancelar");
        _addTranslation("It seems MegaBasterd is streaming video. Do you want to exit?", "Parece que MegaBasterd está retransmitiendo vídeo. ¿Quieres continuar?");
        _addTranslation("It seems MegaBasterd is provisioning down/uploads.\n\nIf you exit now, unprovisioned down/uploads will be lost.\n\nDo you want to continue?", "Parece que MegaBasterd está provisionando transferencias.\n\nSi sales ahora, todas las transferencias no provisionadas se perderán.\n\n¿Quieres continuar?");
        _addTranslation("It seems MegaBasterd is just finishing uploading some files.\n\nIF YOU EXIT NOW, THOSE UPLOADS WILL FAIL.\n\nDo you want to continue?", "Parece que MegaBasterd está finalizando unas subidas.\n\nSI SALES AHORA, ESAS SUBIDAS FALLARÁN.\n\n¿Quieres continuar?");
        _addTranslation("All your current settings and accounts will be deleted after import. (It is recommended to export your current settings before importing). \n\nDo you want to continue?", "Todos tus ajustes y cuentas actuales se perderán después de importar. (Es recomendable guardar tus ajustes actuales antes de importar otros). ¿Quieres continuar?");
        _addTranslation("Only SAVED settings and accounts will be exported. (If you are unsure, it is better to save your current settings and then export them).\n\nDo you want to continue?", "Sólamente los ajustes GUARDADOS se exportarán. (Si no estás seguro, es mejor que guardes antes tus ajustes y que después vuelvas aquí). ¿Quieres continuar?");
        _addTranslation("Master password will be reset and all your accounts will be removed. (THIS CAN'T BE UNDONE)\n\nDo you want to continue?", "La contraseña maestra será reseteada y todas tus cuentas serán eliminadas. (ESTO NO SE PUEDE DESHACER).\n\n¿Quieres continuar?");
        _addTranslation("ALL YOUR SETTINGS, ACCOUNTS AND TRANSFERENCES WILL BE REMOVED. (THIS CAN'T BE UNDONE)\n\nDo you want to continue?", "TODOS TUS AJUSTES, CUENTAS Y TRANSFERENCIAS SERÁN ELIMINADAS. (ESTO NO SE PUEDE DESHACER)\n\n¿Quieres continuar?");
        _addTranslation("Remove all preprocessing, provisioning and waiting downloads?", "¿Eliminar todas las descargas que no están en ejecución?");
        _addTranslation("Warning!", "¡Atención!");
        _addTranslation("Remove all preprocessing, provisioning and waiting uploads?", "¿Eliminar todas las subidas que no están en ejecución?");
        _addTranslation("Please, enter your master password", "Por favor, ingresa tu contraseña maestra");
        _addTranslation("WARNING: if you forget this password, you will have to insert all your accounts again.", "AVISO: si olvidas esta contraseña, tendrás que volver a insertar todas tus cuentas.");
        _addTranslation("New pass:", "Nueva contraseña:");
        _addTranslation("Confirm new:", "Confirmar nueva contraseña:");
        _addTranslation("Verifying your password, please wait...", "Verificando tu contraseña, por favor espera...");
        _addTranslation("Processing your password, please wait...", "Procesando tu contraseña, por favor espera...");
        _addTranslation("Downloads", "Descargas");
        _addTranslation("Uploads", "Subidas");
        _addTranslation("Accounts", "Cuentas");
        _addTranslation("Advanced", "Avanzado");
        _addTranslation("Language:", "Idioma:");
        _addTranslation("English", "Inglés");
        _addTranslation("Spanish", "Español");
        _addTranslation("Traditional Chinese", "Chino tradicional");
        _addTranslation("Loading files, please wait...", "Cargando ficheros, por favor espera...");
        _addTranslation("Checking account quota, please wait...", "Comprobando espacio utilizado de la cuenta, por favor espera...");
        _addTranslation("ERROR checking account quota!", "ERROR al comprobar el espacio utilizado.");
        _addTranslation("If you DO NOT want to transfer some folder or file you can REMOVE it (to select several items at the same time use CTRL + LMOUSE).", "Si no quieres transferir alguna carpeta o archivo puedes quitarlo de la lista (para seleccionar varios elementos al mismo tiempo usa CTRL + RATÓN_IZQ)");
        _addTranslation("Upload name:", "Nombre de la subida:");
        _addTranslation("Account:", "Cuenta:");
        _addTranslation("Folder link detected!", "¡Enlace de carpeta detectado!");
        _addTranslation("File already exists!", "¡El archivo ya existe!");
        _addTranslation("Megabasterd is stopping transferences safely, please wait...", "MegaBasterd está deteniendo las transferencias de forma segura, por favor espera...");
        _addTranslation("Put your MEGA/MegaCrypter/ELC link/s here (one per line):", "Pon aquí tus enlaces de MEGA/MegaCrypter/ELC (uno por línea):");
        _addTranslation("Download folder:", "Carpeta de descarga:");
        _addTranslation("Split size (MBs):", "Tamaño de cada parte (MBs):");
        _addTranslation("File successfully splitted!", "¡Archivo partido correctamente!");
        _addTranslation("File successfully merged!", "¡Archivo juntado correctamente!");
        _addTranslation("File successfuly downloaded!", "¡Archivo descargado correctamente!");
        _addTranslation("Download paused!", "¡Descarga pausada!");
        _addTranslation("Downloading file from mega ...", "Descargando archivo de mega ...");
        _addTranslation("Copy link", "Copiar enlace");
        _addTranslation("Downloading file from mega ", "Descargando archivo de mega ");
        _addTranslation("EXIT NOW", "SALIR AHORA MISMO");
        _addTranslation("Download CANCELED!", "¡Descarga CANCELADA!");
        _addTranslation("Upload CANCELED!", "¡Subida CANCELADA!");
        _addTranslation("Uploading file to mega (", "Subiendo archivo a mega (");
        _addTranslation("Put your MEGA/MegaCrypter/ELC link here in order to get a streaming link:", "Pon aquí tu enlace de MEGA/MegaCrypter/ELC para conseguir un enlace de streaming:");
        _addTranslation("Use this account for streaming:", "Usar esta cuenta para streaming:");
        _addTranslation("Use this account for download:", "Usar esta cuenta para descargar:");
        _addTranslation("Streaming link was copied to clipboard!\nRemember to keep MegaBasterd running in background while playing content.", "¡Enlace de streaming copiado al portapapeles!\nRecuerda dejar abierto MegaBasterd mientras se reproduce el contenido.");
        _addTranslation("CANCEL UPLOAD", "CANCELAR SUBIDA");
        _addTranslation("Upload paused!", "¡Subida pausada!");
        _addTranslation("Stopping download safely before exit MegaBasterd, please wait...", "Deteniendo descarga de forma segura antes de salir, por favor espera...");
        _addTranslation("Stopping upload safely before exit MegaBasterd, please wait...", "Deteniendo subida de forma segura antes de salir, por favor espera...");
        _addTranslation("Starting download, please wait...", "Empezando descarga, por favor espera...");
        _addTranslation("Starting upload, please wait...", "Empezando subida, por favor espera...");
        _addTranslation("Starting download (retrieving MEGA temp link), please wait...", "Empezando descarga (pidiendo enlace temporal de MEGA), por favor espera...");
        _addTranslation("File exists, resuming download...", "El archivo existe, continuando descarga...");
        _addTranslation("Truncating temp file...", "Truncando fichero temporal...");
        _addTranslation("Waiting to check file integrity...", "Esperando para verificar integridad del archivo...");
        _addTranslation("Checking file integrity, please wait...", "Verificando integridad del archivo...");
        _addTranslation("Provisioning download, please wait...", "Aprovisionando descarga, por favor espera...");
        _addTranslation("Waiting to start...", "Esperando para empezar...");
        _addTranslation("Provisioning upload, please wait...", "Aprovisionando subida, por favor espera...");
        _addTranslation("Waiting to start (", "Esperando para empezar (");
        _addTranslation("Creating new MEGA node ... ***DO NOT EXIT MEGABASTERD NOW***", "Creando nuevo nodo de MEGA ... ***NO CIERRES MEGABASTERD EN ESTE MOMENTO***");
        _addTranslation("File successfully uploaded! (", "¡Archivo subido correctamente! (");
        _addTranslation("PAUSE ALL", "PAUSAR TODO");
        _addTranslation("RESUME ALL", "CONTINUAR TODO");
        _addTranslation("MEGA folder link was copied to clipboard!", "¡Enlace de carpeta de MEGA copiado al portapapeles!");
        _addTranslation("MEGA file link was copied to clipboard!", "¡Enlace de fichero de MEGA copiado al portapapeles!");
        _addTranslation("Pausing download ...", "Pausando descarga ...");
        _addTranslation("Pausing upload ...", "Pausando subida ...");
        _addTranslation("File successfully downloaded! (Integrity check PASSED)", "¡Archivo descargado correctamente! (Test de integridad CORRECTO)");
        _addTranslation("File successfully downloaded! (but integrity check CANCELED)", "¡Archivo descargado correctamente! (Test de integridad cancelado)");
        _addTranslation("UPLOAD FAILED! (Empty completion handler!)", "¡SUBIDA FALLIDA! (Manejador de finalización vacío)");
        _addTranslation("UPLOAD FAILED: too many errors", "LA SUBIDA FALLÓ: demasiados errores");
        _addTranslation("UPLOAD FAILED: FATAL ERROR", "LA SUBIDA FALLÓ: ERROR FATAL");
        _addTranslation("BAD NEWS :( File is DAMAGED!", "MALAS NOTICIAS :( El archivo está corrupto!");
        _addTranslation("File temporarily unavailable! (Retrying in ", "¡Archivo temporalmente no disponible! (Reintentando en ");
        _addTranslation(" secs...)", " segundos...)");
        _addTranslation(" (Retrying in ", " (Reintentando en ");
        _addTranslation("Proxy settings", "Ajustes de proxy");
        _addTranslation("Authentication", "Autenticación");
        _addTranslation("Upload info", "Información de la subida");
        _addTranslation("Files", "Archivos");
        _addTranslation("Username:", "Usuario:");
        _addTranslation("Password:", "Contraseña:");
        _addTranslation("Link was copied to clipboard!", "¡Enlace copiado al portapapeles!");
        _addTranslation("Checking if there are previous downloads, please wait...", "Comprobando si existen descargas previas, por favor espera...");
        _addTranslation("Checking if there are previous uploads, please wait...", "Comprobando si existen subidas previas, por favor espera...");
        _addTranslation("Restore window", "Restaurar ventana");
        _addTranslation("EXIT", "SALIR");
        _addTranslation("File successfully downloaded!", "¡Archivo descargado correctamente!");
        _addTranslation("Quota used: ", "Espacio usado: ");
        _addTranslation("Streaming server: ON (port ", "Servidor de streaming: ON (puerto ");
        _addTranslation("MC reverse mode: ON (port ", "MC reverse mode: ON (puerto ");
        _addTranslation("Joining file chunks, please wait...", "Juntando chunks, por favor espera...");
        _addTranslation("Close MegaBasterd when all transfers finish", "Cerrar MegaBasterd cuando todas las transferencias terminen");
        _addTranslation("Use custom temporary directory for chunks storage", "Usar un directorio temporal personalizado para almacenar los chunks");

    }
    
    private void TraditionalChinese() {

        _addTranslation("Use this proxy list (instead of the one included in MegaBasterd) Format is PROXY:PORT", "使用代理伺服器列表(取代MegaBasterd原有的) 格式為PROXY:PORT");
        _addTranslation("Waiting for completion handler ... ***DO NOT EXIT MEGABASTERD NOW***", "等待完成處理 ... ***請勿關閉MEGABASTERD***");
        _addTranslation("Finishing calculating CBC-MAC code (this could take a while) ... ***DO NOT EXIT MEGABASTERD NOW***", "CBC-MAC 結束計算中(這可能需要耗費一些時間) ... ***請勿關閉MEGABASTERD***");
        _addTranslation("Split content in different uploads", "分割內容至不同的上傳佇列");
        _addTranslation("Merge content in the same upload", "合併內容至同一個上傳佇列");
        _addTranslation("How do you want to proceed?", "你想如何進行?");
        _addTranslation("TOP", "置頂");
        _addTranslation("BOTTOM", "置底");
        _addTranslation("Freeze transferences before start", "開始之前停止傳輸");
        _addTranslation("UNFREEZE WAITING TRANSFERENCES", "解除停止等待傳輸");
        _addTranslation("(FROZEN) Waiting to start...", "(凍結) 等待開始...");
        _addTranslation("(FROZEN) Waiting to start (", "(凍結) 等待開始 (");
        _addTranslation("There are a lot of files in this folder.\nNot all links will be provisioned at once to avoid saturating MegaBasterd", "資料夾存在過多檔案\n所有連結將不會一起被配置,以避免MegaBasterd過載");
        _addTranslation("You've tried to login too many times. Wait an hour.", "嘗試次數過多,等待一個小時");
        _addTranslation("MEGA LINK ERROR!", "MEGA連結錯誤!");
        _addTranslation("Please enter 2FA PIN CODE", "請輸入2FA代碼");
        _addTranslation("Enable log file", "啟用LOG");
        _addTranslation("Font:", "字體:");
        _addTranslation("Loading...", "載入中...");
        _addTranslation("DEFAULT", "預設");
        _addTranslation("ALTERNATIVE", "其他");
        _addTranslation("Download latest version", "下載最新版本");
        _addTranslation("PROVISION FAILED", "配置錯誤");
        _addTranslation("Error registering download: file is already downloading.", "下載登錄錯誤:檔案已經下載中");
        _addTranslation("FATAL ERROR! ", "致命錯誤! ");
        _addTranslation("ERROR: FILE NOT FOUND", "錯誤:找不到檔案");
        _addTranslation("Mega link is not valid! ", "不合法的MEGA連結! ");
        _addTranslation("Checking your MEGA accounts, please wait...", "確認你的MEGA帳號中,請稍候...");
        _addTranslation("Check for updates", "檢查更新");
        _addTranslation("Checking, please wait...", "確認中,請稍候...");
        _addTranslation("You have the latest version ;)", "你已經擁有最新版本 ;)");
        _addTranslation("Copy MegaBasterd download URL", "複製MegaBasterd下載連結");
        _addTranslation("Made with love (and with no warranty) by tonikelope.", "tonikelope用愛撰寫(沒有保固)");
        _addTranslation("Yet another unofficial (and ugly) cross-platform MEGA downloader/uploader/streaming suite.", "另一種非官方(有點醜)跨平台的MEGA下載/上傳/串流工具");
        _addTranslation("MEGA URL was copied to clipboard!", "MEGA連結已經複製到剪貼簿!");
        _addTranslation("MegaBasterd NEW VERSION is available! -> ", "MegaBasterd有新版本可用! -> ");
        _addTranslation("Selecting folder...", "資料夾選擇中...");
        _addTranslation("MERGE FILE", "合併檔案");
        _addTranslation("Delete parts after merge", "合併後刪除分割檔");
        _addTranslation("Remove selected", "移除選擇");
        _addTranslation("SPLIT FILE", "分割檔案");
        _addTranslation("Encrypt on disk sensitive information", "加密硬碟敏感資訊");
        _addTranslation("Allow using MEGA accounts for download/streaming", "允許使用MEGA帳號下載/串流");
        _addTranslation("Please wait...", "請稍候...");
        _addTranslation("CANCEL RETRY", "取消重試");
        _addTranslation("SPLITTING FILE...", "分割檔案中...");
        _addTranslation("Open folder", "打開資料夾");
        _addTranslation("RESUME DOWNLOAD", "繼續下載");
        _addTranslation("Close", "關閉");
        _addTranslation("Add files", "增加檔案");
        _addTranslation("IMPORT SETTINGS", "匯入設定");
        _addTranslation("RESET MEGABASTERD", "重置MEGABASTERD");
        _addTranslation("RESET ACCOUNTS", "重置帳號");
        _addTranslation("Verify file integrity (when download is finished)", "檔案完整性驗證(下載完成)");
        _addTranslation("Let's dance, baby", "跳舞吧,寶貝!");
        _addTranslation("Unlock accounts", "解鎖帳號");
        _addTranslation("Use MegaCrypter reverse mode", "使用MegaCrypter反轉模式");
         _addTranslation("Select (any) file part", "選擇(任何)分割檔案");
        _addTranslation("Add folder", "新增資料夾");
        _addTranslation("Adding files, please wait...", "正在新增檔案,請稍候...");
        _addTranslation("Restart", "重新啟動");
        _addTranslation("Use SmartProxy", "使用SmartProxy");
        _addTranslation("PAUSE DOWNLOAD", "暫停下載");
        _addTranslation("Change it", "更改");
        _addTranslation("EXPORT SETTINGS", "匯出設置");
        _addTranslation("Change output folder", "更改輸出資料夾");
        _addTranslation("Add account", "新增帳號");
        _addTranslation("Select file", "選擇檔案");
        _addTranslation("Opening file...", "正在打開檔案...");
        _addTranslation("CANCEL DOWNLOAD", "取消下載");
        _addTranslation("REMOVE ALL EXCEPT THIS", "除此以外的所有內容");
        _addTranslation("Loading DLC, please wait...", "正在載入DLC,請稍候...");
        _addTranslation("Changing output folder...", "更改輸出資料夾...");
        _addTranslation("RESUME UPLOAD", "恢復上傳");
        _addTranslation("Limit upload speed", "限制上傳速度");
        _addTranslation("CANCEL", "取消");
        _addTranslation("Use multi slot download mode", "使用多連線數下載模式");
        _addTranslation("Selecting file...", "正在選擇檔案...");
        _addTranslation("Copy folder link", "複製資料夾連結");
        _addTranslation("Limit download speed", "限制下載速度");
        _addTranslation("REMOVE THIS", "刪除這個");
        _addTranslation("Copy file link", "複製檔案連結");
        _addTranslation("Load DLC container", "載入DLC容器");
        _addTranslation("Adding folder, please wait...", "正在新增資料夾,請稍候...");
        _addTranslation("PAUSE UPLOAD", "暫停上傳");
        _addTranslation("CANCEL CHECK", "取消檢查");
        _addTranslation("Keep temp file", "保留臨時檔案");
        _addTranslation("Use HTTP(S) PROXY", "使用HTTP(S)代理");
        _addTranslation("MERGING FILE...", "合併檔案...");
        _addTranslation("Checking MEGA account...", "正在檢查MEGA帳號...");
        _addTranslation("SAVE", "存檔");
        _addTranslation("New download", "新下載");
        _addTranslation("New upload", "新上傳");
        _addTranslation("New streaming", "新串流");
        _addTranslation("Split file", "分割檔案");
        _addTranslation("Merge file", "合併檔案");
        _addTranslation("Remove all no running downloads", "刪除全部未運行的下載");
        _addTranslation("Remove all no running uploads", "刪除全部未運行的上傳");
        _addTranslation("Edit", "編輯");
        _addTranslation("Settings", "設定");
        _addTranslation("Help", "幫助");
        _addTranslation("About", "關於");
        _addTranslation("Remember for this session", "僅紀錄在此次session");
        _addTranslation("Restore folder data", "恢復資料夾資料");
        _addTranslation("Restoring data, please wait...", "恢復資料,請稍候...");
        _addTranslation("File", "檔案");
        _addTranslation("Hide to tray", "隱藏到工作列");
        _addTranslation("Clear finished", "清除已完成");
        _addTranslation("Exit", "離開");
        _addTranslation("Default slots per file:", "每個檔案的預設連線數:");
        _addTranslation("Note: if you want to download without using a MEGA PREMIUM account you SHOULD enable it. (Slots consume RAM, so use them moderately).", "注意:如果您要下載但不使用MEGA PREMIUM帳號,則應“啟用” (連線數會消耗記憶體,因此請適度使用)");
        _addTranslation("Max speed (KB/s):", "最高速度(KB / s):");
        _addTranslation("Default account:", "預設帳號:");
        _addTranslation("TCP Port:", "TCP埠口:");
        _addTranslation("Note: you MUST \"OPEN\" this port in your router/firewall.","注意:您必須在路由器/防火牆中“打開”該埠口");
        _addTranslation("Note: enable it in order to mitigate bandwidth limit. (Multi slot required).", "注意:啟用它可減輕頻寬限制 (需要多個連線數)");
        _addTranslation("Max parallel downloads:", "最大同時下載:");
        _addTranslation("Max parallel uploads:", "最多同時上傳:");
        _addTranslation("Note: slots consume RAM, so use them moderately.", "注意:連線數會消耗記憶體,因此請適度使用");
        _addTranslation("Note: you can use a (optional) alias for your email addresses -> bob@supermail.com#bob_mail (don't forget to save after entering your accounts).", "注意:您可以使用(可選)別名作為電子郵件地址-> bob@supermail.com#bob_mail(輸入帳號後不要忘記存檔)");
        _addTranslation("Your MEGA accounts:", "您的MEGA帳號:");
        _addTranslation("Your ELC accounts:", "您的ELC帳號:");
        _addTranslation("Note: restart required.", "注意:需要重新啟動");
        _addTranslation("Note: restart might be required.", "注意:可能需要重新啟動");
        _addTranslation("Font ZOOM (%):", "字體縮放(％):");
        _addTranslation("Note: MegaBasterd will use this proxy for ALL connections.", "注意:MegaBasterd將使用此代理進行所有連線");
        _addTranslation("Port:", "埠口:");
        _addTranslation("Settings successfully saved!", "設定已成功存檔！");
        _addTranslation("Settings successfully imported!", "設定已成功匯入！");
        _addTranslation("Settings successfully exported!", "設定成功匯出！");
        _addTranslation("Settings successfully reset!", "設定成功重置！");
        _addTranslation("There were errors with some accounts (email and/or password are/is wrong). Please, check them:\n\n", "某些帳號存在錯誤(電子郵件和/或密碼錯誤)請檢查它們:\n\n");
        _addTranslation("Settings saved", "設定已存檔");
        _addTranslation("Settings imported", "設定已匯入");
        _addTranslation("Settings exported", "設定匯出");
        _addTranslation("Settings reset", "設定重置");
        _addTranslation("MegaBasterd will restart", "MegaBasterd將重新啟動");
        _addTranslation("Restart required", "需要重新啟動");
        _addTranslation("File is not readable!", "檔案無法讀取！");
        _addTranslation("Folder is not readable!", "資料夾無法讀取！");
        _addTranslation("BAD PASSWORD!", "密碼錯誤！");
        _addTranslation("Passwords does not match!", "密碼不符合！");
        _addTranslation("Please, paste a Mega/MegaCrypter/ELC link!", "請貼上Mega / MegaCrypter / ELC連結！");
        _addTranslation("Yes", "是");
        _addTranslation("Cancel", "取消");
        _addTranslation("It seems MegaBasterd is streaming video. Do you want to exit?", "MegaBasterd似乎正在串流影片,你想離開嗎？");
        _addTranslation("It seems MegaBasterd is provisioning down/uploads.\n\nIf you exit now, unprovisioned down/uploads will be lost.\n\nDo you want to continue?", "MegaBasterd似乎正在預配置下載/上傳\n\n如果您現在離開,未配置的下載/上傳將遺失\n\n您要繼續嗎？");
        _addTranslation("It seems MegaBasterd is just finishing uploading some files.\n\nIF YOU EXIT NOW, THOSE UPLOADS WILL FAIL.\n\nDo you want to continue?", "MegaBasterd似乎正在完成一些檔案的上傳\n\n如果現在離開,上傳將會失敗\n\n您要繼續嗎？");
        _addTranslation("All your current settings and accounts will be deleted after import. (It is recommended to export your current settings before importing). \n\nDo you want to continue?", "匯入後,所有當前設定和帳號將被刪除 (建議在匯入之前匯出當前設置) \n\n您要繼續嗎？");
        _addTranslation("Only SAVED settings and accounts will be exported. (If you are unsure, it is better to save your current settings and then export them).\n\nDo you want to continue?", "僅已儲存的設置和帳號將被匯出 (如果不確定,最好儲存當前設置,然後將其匯出)\n\n您要繼續嗎？");
        _addTranslation("Master password will be reset and all your accounts will be removed. (THIS CAN'T BE UNDONE)\n\nDo you want to continue?", "主要密碼將被重置,您的所有帳號將被刪除 (無法復原)\n\n您要繼續嗎？");
        _addTranslation("ALL YOUR SETTINGS, ACCOUNTS AND TRANSFERENCES WILL BE REMOVED. (THIS CAN'T BE UNDONE)\n\nDo you want to continue?", "您的所有設定,帳號和傳輸資訊都將被刪除 (無法復原)\n\n您要繼續嗎？");
        _addTranslation("Remove all preprocessing, provisioning and waiting downloads?", "刪除所有預處理中,配置中和等待中的下載？");
        _addTranslation("Warning!", "警告！");
        _addTranslation("Remove all preprocessing, provisioning and waiting uploads?", "刪除所有預處理中,配置中和等待中的上傳？");
        _addTranslation("Please, enter your master password", "請輸入您的主要密碼");
        _addTranslation("WARNING: if you forget this password, you will have to insert all your accounts again.", "警告:如果您忘記了密碼,則必須再次輸入所有帳號");
        _addTranslation("New pass:", "新密碼:");
        _addTranslation("Confirm new:", "確認新密碼:");
        _addTranslation("Verifying your password, please wait...", "驗證您的密碼,請稍候...");
        _addTranslation("Processing your password, please wait...", "正在處理您的密碼,請稍候...");
        _addTranslation("Downloads", "下載");
        _addTranslation("Uploads", "上載");
        _addTranslation("Accounts", "帳號");
        _addTranslation("Advanced", "進階");
        _addTranslation("Language:", "語系:");
        _addTranslation("English", "英文");
        _addTranslation("Spanish", "西班牙文");
        _addTranslation("Traditional Chinese", "繁體中文");
        _addTranslation("Loading files, please wait...", "正在載入檔案,請稍候...");
        _addTranslation("Checking account quota, please wait...", "正在檢查帳號配額,請稍候...");
        _addTranslation("ERROR checking account quota!", "檢查帳號配額時出錯！");
        _addTranslation("If you DO NOT want to transfer some folder or file you can REMOVE it (to select several items at the same time use CTRL + LMOUSE).", "如果您不想傳輸某些資料夾或檔案,則可以將其刪除(要同時選擇多個項目,請使用CTRL + 滑鼠右鍵)");
        _addTranslation("Upload name:", "上傳名稱:");
        _addTranslation("Account:", "帳號:");
        _addTranslation("Folder link detected!", "檢測到資料夾連結！");
        _addTranslation("File already exists!", "檔案已存在！");
        _addTranslation("Megabasterd is stopping transferences safely, please wait...", "Megabasterd正在安全地停止傳輸,請稍候...");
        _addTranslation("Put your MEGA/MegaCrypter/ELC link/s here (one per line):", "將您的MEGA / MegaCrypter / ELC連結放在此處(每行一個):");
        _addTranslation("Download folder:", "下載資料夾:");
        _addTranslation("Split size (MBs):", "分割大小(MB):");
        _addTranslation("File successfully splitted!", "檔案成功分割！");
        _addTranslation("File successfully merged!", "檔案成功合併！");
        _addTranslation("File successfuly downloaded!", "檔案下載成功！");
        _addTranslation("Download paused!", "下載已暫停！");
        _addTranslation("Downloading file from mega ...", "正在從mega下載檔案...");
        _addTranslation("Copy link", "複製連結");
        _addTranslation("Downloading file from mega ", "從mega下載檔案");
        _addTranslation("EXIT NOW", "立即退出");
        _addTranslation("Download CANCELED!", "下載已取消！");
        _addTranslation("Upload CANCELED!", "上載已取消！");
        _addTranslation("Uploading file to mega (", "上載檔案至mega(");
        _addTranslation("Put your MEGA/MegaCrypter/ELC link here in order to get a streaming link:", "將您的MEGA / MegaCrypter / ELC連結放在此處以獲得串流連結:");
        _addTranslation("Use this account for streaming:", "使用此帳號進行串流:");
        _addTranslation("Use this account for download:", "使用此帳號進行下載:");
        _addTranslation("Streaming link was copied to clipboard!\nRemember to keep MegaBasterd running in background while playing content.", "串流連結已複製到剪貼簿！\n請記住,播放內容時需保持MegaBasterd在背景執行");
        _addTranslation("CANCEL UPLOAD", "取消上傳");
        _addTranslation("Upload paused!", "上傳已暫停！");
        _addTranslation("Stopping download safely before exit MegaBasterd, please wait...", "正在安全地停止下載,完成後會離開MegaBasterd,請稍候...");
        _addTranslation("Stopping upload safely before exit MegaBasterd, please wait...", "正在安全地停止上傳,完成後會離開MegaBasterd,請稍候...");
        _addTranslation("Starting download, please wait...", "開始下載,請稍候...");
        _addTranslation("Starting upload, please wait...", "開始上傳,請稍候...");
        _addTranslation("Starting download (retrieving MEGA temp link), please wait...", "正在開始下載(正在取得MEGA臨時連結),請稍候...");
        _addTranslation("File exists, resuming download...", "檔案存在,正在恢復下載...");
        _addTranslation("Truncating temp file...", "截斷臨時檔案...");
        _addTranslation("Waiting to check file integrity...", "正在等待檢查檔案完整性...");
        _addTranslation("Checking file integrity, please wait...", "正在檢查檔案完整性,請稍候...");
        _addTranslation("Provisioning download, please wait...", "正在下載配置檔案,請稍候...");
        _addTranslation("Waiting to start...", "等待開始...");
        _addTranslation("Provisioning upload, please wait...", "正在配置上傳,請稍候...");
        _addTranslation("Waiting to start (", "等待開始(");
        _addTranslation("Creating new MEGA node ... ***DO NOT EXIT MEGABASTERD NOW***", "正在創造新的MEGA節點... ***現在不要關閉MEGABASTERD ***");
        _addTranslation("File successfully uploaded! (", "檔案成功上傳！ (");
        _addTranslation("PAUSE ALL", "全部暫停");
        _addTranslation("RESUME ALL", "恢復全部");
        _addTranslation("MEGA folder link was copied to clipboard!", "MEGA資料夾連結已複製到剪貼簿！");
        _addTranslation("MEGA file link was copied to clipboard!", "MEGA檔案連結已複製到剪貼簿！");
        _addTranslation("Pausing download ...", "暫停下載中...");
        _addTranslation("Pausing upload ...", "暫停上載中...");
        _addTranslation("File successfully downloaded! (Integrity check PASSED)", "檔案下載成功！ (完整性檢查通過)");
        _addTranslation("File successfully downloaded! (but integrity check CANCELED)", "檔案下載成功！ (但完整性檢查已取消)");
        _addTranslation("UPLOAD FAILED! (Empty completion handler!)", "上傳失敗！ (無完成處理程序！)");
        _addTranslation("UPLOAD FAILED: too many errors", "失敗:錯誤太多");
        _addTranslation("UPLOAD FAILED: FATAL ERROR", "上載失敗:致命錯誤");
        _addTranslation("BAD NEWS :( File is DAMAGED!", "壞消息:(檔案已損壞！");
        _addTranslation("File temporarily unavailable! (Retrying in ", "檔案暫時不可用！ (重試中");
        _addTranslation(" secs...)", " 秒...)");
        _addTranslation(" (Retrying in ", " (重試中");
        _addTranslation("Proxy settings", "代理設定");
        _addTranslation("Authentication", "認證方式");
        _addTranslation("Upload info", "上載資料");
        _addTranslation("Files", "檔案");
        _addTranslation("Username:", "使用者名稱:");
        _addTranslation("Password:", "密碼:");
        _addTranslation("Link was copied to clipboard!", "連結已複製到剪貼簿！");
        _addTranslation("Checking if there are previous downloads, please wait...", "正在檢查以前是否有下載過,請稍候...");
        _addTranslation("Checking if there are previous uploads, please wait...", "正在檢查是否有以前的上傳過,請稍候...");
        _addTranslation("Restore window", "還原視窗");
        _addTranslation("EXIT", "離開");
        _addTranslation("File successfully downloaded!", "檔案下載成功！");
        _addTranslation("Quota used: ", "已使用的配額:");
        _addTranslation("Streaming server: ON (port ", "串流伺服器:ON(埠口");
        _addTranslation("MC reverse mode: ON (port ", "MC反向模式:ON(埠口");
        _addTranslation("Joining file chunks, please wait...", "正在加入檔案區塊,請稍候...");
        _addTranslation("Close MegaBasterd when all transfers finish", "所有傳輸完成後關閉MegaBasterd");
        _addTranslation("Use custom temporary directory for chunks storage", "使用自定臨時目錄進行區塊儲存");

    }

    private void _addTranslation(String key, String val) {

        if (_rosetta.putIfAbsent(key, val) != null) {

            Logger.getLogger(MainPanel.class.getName()).log(Level.WARNING, "Rosetta: {0} aready exists!", new Object[]{key});
        }
    }

    public String translate(String orig) {

        return _rosetta.containsKey(orig) ? _rosetta.get(orig) : orig;
    }

    private static class LazyHolder {

        private static final LabelTranslatorSingleton INSTANCE = new LabelTranslatorSingleton();

        private LazyHolder() {
        }
    }
}
