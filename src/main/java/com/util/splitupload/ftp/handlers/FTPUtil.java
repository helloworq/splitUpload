package com.util.splitupload.ftp.handlers;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FTPUtil {

    public static FTPClient connectFtpServer(String addr, int port, String username, String password, String controlEncoding) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.setControlEncoding(controlEncoding);
            ftpClient.connect(addr, port);
            if (null == (username)) {
                ftpClient.login("Anonymous", "");
            } else {
                ftpClient.login(username, password);
            }

            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.abort();
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ftpClient;
    }

    public static FTPClient closeFTPConnect(FTPClient ftpClient) {
        try {
            if (ftpClient != null && ftpClient.isConnected()) {
                ftpClient.abort();
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ftpClient;
    }

    public static void downloadSingleFile(FTPClient ftpClient) {
        try {
            ftpClient.retrieveFile("/Netty权威指南.pdf",
                    Files.newOutputStream(Paths.get("C:\\Users\\***\\Desktop\\aaa.pdf"), StandardOpenOption.CREATE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void storeFile(FTPClient ftpClient) throws IOException {
        try {
            //注意ccc.pdf需要自定义名字，因为服务器如果已存在同名文件则会直接跳过上传步骤
            ftpClient.storeFile("/ccc.pdf", Files.newInputStream(Paths.get("C:\\Users\\***\\Desktop\\aaa.pdf")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        FTPClient ftpClient = FTPUtil.connectFtpServer(
                "127.0.0.1",
                21,
                "admin",
                "123456",
                "utf-8");

        downloadSingleFile(ftpClient);
        //storeFile(ftpClient);

        closeFTPConnect(ftpClient);
    }
}