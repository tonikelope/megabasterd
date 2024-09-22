package com.tonikelope.megabasterd;
import static com.tonikelope.megabasterd.MiscTools.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RemoteAPI {
    private static final Logger LOG = Logger.getLogger(ChunkDownloader.class.getName());
    private final MainPanel _main_panel;
    private final DownloadManager _download_manager;
    private final MegaAPI ma = new MegaAPI();
    public boolean enabled = false;
    public int port = 0;
    public RemoteAPI(MainPanel main_panel) {
        _main_panel = main_panel;
        _download_manager = _main_panel.getDownload_manager();

        try{
            String enable_remote_api_val = DBTools.selectSettingValue("enable_remote_api");
            if (enable_remote_api_val != null) {
                enabled = enable_remote_api_val.equals("yes");
            }

            String remote_api_p = DBTools.selectSettingValue("remote_api_port");
            port = Integer.parseInt(remote_api_p);

            // do not start if not enabled or port is 0
            if(!enabled || port == 0) return;
            port(port); // Set the port number
        } catch (Exception ex){
            LOG.log(Level.SEVERE, "Unable to start remote api server.", ex.getMessage());
        }

        Gson gson = new Gson();

        get("/status", (req, res) -> {
            res.type("application/json");

            // system status
            Map<String, Object> status = new HashMap<>();
            status.put("size", _download_manager.get_total_size());
            status.put("loaded", _download_manager.get_total_progress());;
            status.put("running", _download_manager.getTransference_running_list().size() > 0 && !_download_manager.isPaused_all());

            // get downloads
            ArrayList<Map<String, Object>> downloads = new ArrayList<>();
            for (Download dl : getDownloads()) {
                String dlStatus = "Unknown";
                boolean finished = false;

                String statusText = dl.getView().getStatus_label().getText();
                switch(statusText) {
                    case "Provisioning download, please wait...":
                    case "Starting download, please wait...":
                    case "Starting download (retrieving MEGA temp link), please wait...":
                    case "File exists, resuming download...":
                    case "Truncating temp file...":
                        dlStatus = "Starting...";
                        break;
                    case "Download paused!":
                        dlStatus = "Paused";
                        break;
                    case "Download CANCELED!":
                        dlStatus = "Canceled";
                        break;
                    case "Waiting to check file integrity...":
                    case "Checking file integrity, please wait...":
                    case "Download finished. Joining file chunks, please wait...":
                        dlStatus = "Decrypting";
                        break;
                    case "File successfully downloaded!":
                    case "File successfully downloaded! (Integrity check PASSED)":
                    case "File successfully downloaded! (but integrity check CANCELED)":
                        dlStatus = "Finished";
                        finished = true;
                        break;
                }

                if (dl.getView().getStatus_label().getText().startsWith("Downloading file from mega")){
                    dlStatus = "Download";
                }

                if (dl.isStatusError()){
                    dlStatus = "Error";
                }

                downloads.add(createDownloadStatus(dl.getUrl(), dl.getFile_name(), getDownloadPath(dl),
                        dl.getFile_size(), dl.getProgress(), dl.getFile_size() == dl.getProgress() ? 0 : dl.getSpeed(),
                        dlStatus, dl.getStatus_error(), finished));
            }
            status.put("downloads", downloads);

            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(status);
        });

        post("/pause", (req, res) -> {
            res.type("application/json");

            _download_manager.pauseAll();
            return "{\"messsage\": \"Paused all downloads\"}";
        });

        post("/resume", (req, res) -> {
            res.type("application/json");

            _download_manager.resumeAll();
            return "{\"messsage\": \"Resumed all downloads\"}";
        });

        post("/start", (req, res) -> {
            try {

                res.type("application/json");

                // Parse the JSON body
                JsonObject jsonBody = JsonParser.parseString(req.body()).getAsJsonObject();
                if (!jsonBody.has("urls")) {
                    res.status(400); // Bad Request
                    return gson.toJson(new ErrorResponse("Missing required parameters."));
                }

                String linksText = jsonBody.get("urls").getAsString();
                Set<String> urls = parseUrls(linksText);

                if (urls.isEmpty()) {
                    res.status(400); // Bad Request
                    return gson.toJson(new ErrorResponse("No mega files links found. Note: folder links not supported"));
                }

                String downloadPath = _main_panel.getDefault_download_path();
                if(jsonBody.has("dest")){
                    downloadPath = Paths.get(_main_panel.getDefault_download_path(), jsonBody.get("dest").getAsString()).toString();
                    File path = new File(downloadPath);
                    if(!path.exists()) path.mkdirs();
                }

                // add urls
                for (String url : urls) {
                    Download download = new Download(
                            _main_panel, ma, url, downloadPath,
                            null, null, null, null, null, _main_panel.isUse_slots_down(),
                            false, _main_panel.isUse_custom_chunks_dir() ? _main_panel.getCustom_chunks_dir() : null,false);
                    _download_manager.getTransference_provision_queue().add(download);
                    _download_manager.secureNotify();
                }

                return "{\"message\": \""+urls.size()+" Downloads Started, "+urls.toString()+"\"}";
            } catch (Exception ex){
                res.status(500);
                return "{\"message\": \""+ex.toString()+"\"}";
            }
        });

        post("/stop", (req, res) -> {
            try {
                res.type("application/json");

                // Parse the JSON body
                JsonObject jsonBody = JsonParser.parseString(req.body()).getAsJsonObject();
                if (!jsonBody.has("url")) {
                    res.status(400); // Bad Request
                    return gson.toJson(new ErrorResponse("Missing required parameters."));
                }

                String downloadUrl = jsonBody.get("url").getAsString();

                boolean deleteFiles = false;
                if(jsonBody.has("delete")) deleteFiles = jsonBody.get("delete").getAsBoolean();

                Download download = findDownloadByURL(downloadUrl);
                if(download == null) {
                    res.status(404);
                    return "{\"message\": \"Download not found.\"}";
                } else {
                    // stop download
                    download.getView().getPause_button().doClick();
                    download.getView().getKeep_temp_checkbox().setSelected(!deleteFiles);
                    download.getView().getStop_button().doClick();

                    // wait for clear but then remove download from list with 30-second timeout
                    long startTime = System.currentTimeMillis();
                    while (!download.getView().getClose_button().isVisible()) {
                        if (System.currentTimeMillis() - startTime > 30000) {
                            break; // Timeout after 30 seconds
                        }
                        Thread.sleep(100);
                    }

                    // If the close button is visible, click it
                    if (download.getView().getClose_button().isVisible()) {
                        download.getView().getClose_button().doClick();

                        // delete files if delete = true, is not default and folder is empty
                        Path downloadPath = Paths.get(download.getDownload_path());
                        boolean deleted = false;
                        if(deleteFiles && !downloadPath.toString().equals(_main_panel.getDefault_download_path())) {
                            String downloadFolder = getDownloadPath(download);

                            // delete file
                            Path filePath = Paths.get(_main_panel.getDefault_download_path(), downloadFolder, download.getFile_name());
                            File file = new File(filePath.toString());
                            if(file.exists()) file.delete();

                            // delete folders if empty
                            ArrayList<String> pathsToCheck = new ArrayList<>();
                            Path currentPath = Paths.get(_main_panel.getDefault_download_path());
                            for (String folder : downloadFolder.split("/")) {
                                currentPath = Paths.get(currentPath.toString(), folder);
                                pathsToCheck.add(currentPath.toString());
                            }

                            Collections.reverse(pathsToCheck);
                            for (String folder : pathsToCheck) {
                                Path folderPath = Paths.get(folder);
                                if (isFolderEmpty(folderPath)) {
                                    Files.delete(folderPath);
                                    deleted = true;
                                }
                            }
                        }

                        if(deleted){
                            return "{\"message\": \"Removed download and deleted\"}";
                        }
                        return "{\"message\": \"Removed download\"}";
                    } else {
                        return "{\"message\": \"Failed to remove download within timeout.\"}";
                    }
                }
            } catch (Exception ex){
                res.status(500);
                return "{\"message\": \""+ex.toString()+"\"}";
            }
        });


        post("/rename", (req, res) -> {
            try {
                res.type("application/json");

                // Parse the JSON body
                JsonObject jsonBody = JsonParser.parseString(req.body()).getAsJsonObject();
                if (!jsonBody.has("url") || !jsonBody.has("url")) {
                    res.status(400); // Bad Request
                    return gson.toJson(new ErrorResponse("Missing required parameters."));
                }

                String downloadUrl = jsonBody.get("url").getAsString();
                String newName = jsonBody.get("newName").getAsString();
                Download download = findDownloadByURL(downloadUrl);
                if(download == null) {
                    res.status(404);
                    return "{\"message\": \"Download not found.\"}";
                }

                boolean renamed = download.setFile_name(newName);
                if(renamed) {
                    return "{\"message\": \"Renamed download\"}";
                } else {
                    res.status(400);
                    return "{\"message\": \"Unable to rename download\"}";
                }

            } catch (Exception ex){
                res.status(500);
                return "{\"message\": \""+ex.toString()+"\"}";
            }
        });

        // Catch-all for 404 Not Found
        notFound((req, res) -> {
            res.type("text/plain");
            return "404 Not Found";
        });
    }

    static class ErrorResponse {
        String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }

    private Download findDownloadByURL(String url)
    {
        Download download = null;
        for (Download dl : getDownloads()) {
            if (Objects.equals(dl.getUrl(), url)) {
                download = dl;
                break;
            }
        }

        return download;
    }

    private String getDownloadPath(Download dl)
    {
        Path defaultDownloadPath = Paths.get(_main_panel.getDefault_download_path());
        Path dlFullPath = Paths.get(dl.getDownload_path());

        ArrayList<String> dlPathList = new ArrayList<>();
        for (int i = 0; i < dlFullPath.getNameCount(); i++) {
            if(i > defaultDownloadPath.getNameCount()-1){
                dlPathList.add(dlFullPath.getName(i).toString());
            }
        }

        return String.join("/", dlPathList);
    }

    public static boolean isFolderEmpty(Path folderPath) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(folderPath)) {
            return !dirStream.iterator().hasNext(); // If no entries exist, folder is empty
        } catch (IOException e) {
            return false; // Return false in case of an error
        }
    }

    private Set<String> parseUrls(String linksText){
        String link_data = MiscTools.extractMegaLinksFromString(linksText);
        Set<String> urls = new HashSet(findAllRegex("(?:https?|mega)://[^\r\n]+(#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n/]+", link_data, 0));

        // remove folder links (not supported yet)
        if (!urls.isEmpty()) {
            Set<String> folder_file_links = new HashSet(findAllRegex("(?:https?|mega)://[^\r\n]+#F\\*[^\r\n!]*?![^\r\n!]+![^\\?\r\n/]+", link_data, 0));
            if (!folder_file_links.isEmpty()) {
                ArrayList<String> nlinks = ma.GENERATE_N_LINKS(folder_file_links);
                urls.removeAll(folder_file_links);
                urls.addAll(nlinks);
            }
        }
        urls.removeIf(url -> findFirstRegex("#F!", url, 0) != null);

        // validate url exists
        for (String url : urls) {
            try{
                ma.getMegaFileMetadata(url);
            } catch (Exception ex){
                urls.remove(url);
            }
        }

        return urls;
    }

    private ArrayList<Download> getDownloads(){
        ArrayList<Download> downloads = new ArrayList<>();

        // gets all the download components from the list
        Component[] components = _download_manager.getScroll_panel().getComponents();
        for (Component component : components) {
            try {
                if (component instanceof DownloadView) {
                    downloads.add(((DownloadView) component).getDownload());
                }
            } catch (Exception ignored) {}
        }

        return downloads;
    }

    // Helper method to create a DownloadStatus JSONObject
    public static Map<String, Object> createDownloadStatus(String url, String name, String path, long size, long bytesLoaded, long speed, String status, String error, Boolean finished) {
        Map<String, Object> downloadStatus = new HashMap<>();
        downloadStatus.put("url", url);
        downloadStatus.put("name", name);
        downloadStatus.put("path", path);
        downloadStatus.put("bytesTotal", size);
        downloadStatus.put("bytesLoaded", bytesLoaded);
        downloadStatus.put("speed", speed);
        downloadStatus.put("status", status);
        downloadStatus.put("finished", finished);
        downloadStatus.put("error", error);
        return downloadStatus;
    }
}
