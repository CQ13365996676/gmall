package com.atguigu.gmall.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @Description
 * @auther CQ
 * @create 2019-12-28 下午 7:17
 */
@CrossOrigin
@RestController
public class FileUploadController {

    @Value("${fileServer.url}")
    private String fastDFSUrl;

    /**
     * 上传图片到fastDFS服务器中
     * http://localhost:8082/fileUpload
     * @param file
     * @return
     */
    @RequestMapping("/fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {
        //1.初始化返回的URL变量
        String imgPath = fastDFSUrl;
        //2.初始化操作
        String initFile = this.getClass().getResource("/tracker.conf").getFile();
        ClientGlobal.init(initFile);
        TrackerClient trackerClient=new TrackerClient();
        TrackerServer trackerServer=trackerClient.getConnection();
        StorageClient storageClient=new StorageClient(trackerServer,null);
        //3.获取上传的文件全名
        String orginalFilename=file.getOriginalFilename();
        //4.获取文件后缀名
        String extName = StringUtils.substringAfterLast(orginalFilename, ".");
        //5.上传图片至服务器
        String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
        if(upload_file!=null && upload_file.length>0){
            //6.获取图片地址
            for (String s : upload_file) {
                imgPath+="/"+s;
            }
            return imgPath;
        }
        return null;
    }

}
