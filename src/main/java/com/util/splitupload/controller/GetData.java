package com.util.splitupload.controller;

import com.util.splitupload.entiy.FileInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
public class GetData {

    private static final String uploadPath = "C:\\Users\\lei.zhou\\Desktop\\split\\";//上传文件的保存路径
    //最小化依赖，采用内存缓存临时数据，不再使用数据库持久化
    private static final Map<String, FileInfo> FileInfoCache = new HashMap<>();

    /**
     * 处理上传文件
     *
     * @param fileMd5
     * @param file
     * @param fileSize
     * @param position
     * @param fileName
     * @throws IOException
     */
    @PostMapping("/upload")
    public void upload(@RequestParam("fileMd5") String fileMd5,
                       @RequestParam("file") MultipartFile file,
                       @RequestParam("fileSize") String fileSize,
                       @RequestParam("position") String position,
                       @RequestParam("fileName") String fileName,
                       HttpServletResponse response) throws IOException {

        FileInfo fileInfo = FileInfoCache.get(fileMd5);

        if (Objects.nonNull(fileInfo)) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(fileInfo.getFilePath(), "rw");
            //判断本地存储文件是否完整存储，是，则存储上传信息，不是则开始上传
            if (randomAccessFile.length() == Long.parseLong(fileSize)) {
                randomAccessFile.close();
                FileInfoCache.put(fileMd5, fileInfo);
                response.setStatus(201);
            } else {
                //上传进度中
                randomAccessFile.seek(Long.parseLong(position));
                randomAccessFile.write(file.getBytes());
                fileInfo.setFileSize(fileInfo.getFileSize() + file.getSize());
                try {
                    //防止redis保存数据时出错丢失进度
                    FileInfoCache.put(fileMd5, fileInfo);
                } catch (Exception e) {
                    System.out.println("redis缓存出错!");
                    FileInfoCache.put(fileMd5, fileInfo);
                    randomAccessFile.close();
                }
                //判断是否完成存储
                if (randomAccessFile.length() == Long.parseLong(fileSize)) {
                    FileInfoCache.put(fileMd5, fileInfo);
                    response.setStatus(201);
                }
                randomAccessFile.close();
            }
        } else {
            //初次上传
            File filePrev = new File(uploadPath + fileMd5 + getFileSuffix(fileName));
            if (!filePrev.exists()) {
                filePrev.createNewFile();
            }

            RandomAccessFile randomAccessFile = new RandomAccessFile(filePrev.getPath(), "rw");
            randomAccessFile.seek(Long.parseLong(position));
            randomAccessFile.write(file.getBytes());
            randomAccessFile.close();

            FileInfo fileInfoPrev = new FileInfo();
            fileInfoPrev.setFilePath(filePrev.getPath());//文件存储路径
            fileInfoPrev.setFileName(fileName);//文件原始名
            fileInfoPrev.setFileSize(file.getSize());//文件大小
            fileInfoPrev.setFileMd5(fileMd5);
            FileInfoCache.put(fileMd5, fileInfoPrev);
        }
    }

    /**
     * 依据传入的md5获取此文件在服务器上的大小
     * <p>
     * 可能是初次上传时请求，也可能是上传过程中的请求，也可能是断了很久之后再次上传的请求
     *
     * @param fileMd5
     * @return
     */
    @PostMapping("/getSize")
    public String getSize(@RequestParam("fileMd5") String fileMd5) {
        //启用redis缓存支持，减少对数据的请求次数
        FileInfo fileInfo = FileInfoCache.get(fileMd5);
        return Objects.nonNull(fileInfo) ? String.valueOf(fileInfo.getFileSize()) : "0";
    }

    public String getFileSuffix(String fileName) {
        int position = fileName.lastIndexOf(".");
        return fileName.substring(position);
    }
}
