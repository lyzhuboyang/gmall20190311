package com.zby.gmall.gmallmanageweb.controller;

import com.zby.gmall.commonutil.data.R;
import org.apache.commons.lang3.StringUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@CrossOrigin
public class FileUploadController {

    @Value("${fileServer.url}")
    private String pathUrl;

    @RequestMapping("fileUpload")
    public R fileUpload(MultipartFile file) {
        String imgUrl = pathUrl;
        try {
            if (file != null) {
                String configFile = this.getClass().getResource("/tracker.conf").getFile();
                ClientGlobal.init(configFile);
                TrackerClient trackerClient = new TrackerClient();
                TrackerServer trackerServer = trackerClient.getTrackerServer();
                StorageClient storageClient = new StorageClient(trackerServer, null);
                String orginalFilename = file.getOriginalFilename();
                String extName = StringUtils.substringAfterLast(orginalFilename, ".");
                String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
                for (int i = 0; i < upload_file.length; i++) {
                    String path = upload_file[i];
                    imgUrl += "/" + path;
                    //http://192.168.83.220/group1/M00/00/00/wKhT3F-NlyWAahxvAAMbP2CmpjE759.jpg
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok().data("imageUrl", imgUrl);
    }


}
