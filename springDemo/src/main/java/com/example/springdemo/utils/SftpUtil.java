package com.example.springdemo.utils;


import com.example.springdemo.config.SftpConfig;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * @Author: Lorem Moon
 * @Date: 2022/6/21 16:15
 * SFTP 工具类
 */
public class SftpUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SftpUtil.class);

    private long count;
    /**
     * 已经连接次数
     */
    private long count1 = 0;

    private long sleepTime;

    private int connectionTimeout;

    private Session session;

    private JSch jsch;

    /**
     * 连接sftp服务器
     *
     * @return
     */
    public ChannelSftp connect(SftpConfig sftpConfig) {
        ChannelSftp sftp = null;
        connectionTimeout = 5000;
        try {
            jsch = new JSch();
            jsch.getSession(sftpConfig.getUsername(), sftpConfig.getHostname(), sftpConfig.getPort());
            Session sshSession = jsch.getSession(sftpConfig.getUsername(), sftpConfig.getHostname(), sftpConfig.getPort());
            LOGGER.info("Session created ... UserName={} host={} port={}", sftpConfig.getUsername(), sftpConfig.getHostname(), sftpConfig.getPort());
            sshSession.setPassword(sftpConfig.getPassword());
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            sshConfig.put("PreferredAuthentications", "password");
            sshSession.setConfig(sshConfig);
            sshSession.connect(connectionTimeout);
//            sshSession = getSession(sftpConfig, sshConfig);
            LOGGER.info("Session connected ...");
            LOGGER.info("Opening Channel ...");
            ChannelSftp channel = (ChannelSftp) sshSession.openChannel("sftp");
            channel.connect(connectionTimeout);
//            channel.connect();
            sftp = (ChannelSftp) channel;
            LOGGER.info("登录成功");
        } catch (Exception e) {
            try {
                count1 += 1;
                if (count == count1) {
                    throw new RuntimeException(e);
                }
//                Thread.sleep(sleepTime);
                LOGGER.info("重新连接....");
                connect(sftpConfig);
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        }
        return sftp;
    }

    /**
     * 上传文件
     *
     * @param directory  上传的目录
     * @param uploadFile 要上传的文件
     * @param sftpConfig
     */
    public void upload(String directory, String uploadFile, SftpConfig sftpConfig) {
        LOGGER.info("directory: " + directory);
        LOGGER.info("uploadFile: " + uploadFile);
        ChannelSftp sftp = connect(sftpConfig);
        try {
            sftp.cd(directory);
            LOGGER.debug(directory);
        } catch (SftpException e) {
            try {
                sftp.mkdir(directory);
                sftp.cd(directory);
                LOGGER.info("ftp创建文件路径成功！" + directory);
            } catch (SftpException e1) {
                e1.printStackTrace();
                LOGGER.error(e1.toString());
                throw new RuntimeException("ftp创建文件路径失败" + directory);
            }
        }
        File file = new File(uploadFile);
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            sftp.put(inputStream, file.getName());
        } catch (Exception e) {
            throw new RuntimeException("sftp异常" + e);
        } finally {
            disConnect(sftp);
            closeStream(inputStream, null);
        }
    }

    /**
     * 下载文件
     *
     * @param directory    下载目录
     * @param downloadFile 下载的文件
     * @param saveFile     存在本地的路径
     * @param sftpConfig
     */
    public void download(String directory, String downloadFile, String saveFile, SftpConfig sftpConfig) {
        OutputStream output = null;
        try {
            File localDirFile = new File(saveFile);
            // 判断本地目录是否存在，不存在需要新建各级目录
            if (!localDirFile.exists()) {
                localDirFile.mkdirs();
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("开始获取远程文件:[{}]---->[{}]", new Object[]{directory, saveFile});
            }
            ChannelSftp sftp = connect(sftpConfig);
            sftp.cd(directory);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("打开远程文件:[{}]", new Object[]{directory});
            }
            output = new FileOutputStream(new File(saveFile.concat(File.separator).concat(downloadFile)));
            sftp.get(downloadFile, output);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("文件下载成功");
            }
            disConnect(sftp);
        } catch (Exception e) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("文件下载出现异常，[{}]", e);
            }
            throw new RuntimeException("文件下载出现异常，[{}]", e);
        } finally {
            closeStream(null, output);
        }
    }

    /**
     * 下载远程文件夹下的所有文件
     *
     * @param remoteFilePath
     * @param localDirPath
     * @throws Exception
     */
    public void getFileDir(String remoteFilePath, String localDirPath, SftpConfig sftpConfig) throws Exception {
        File localDirFile = new File(localDirPath);
        // 判断本地目录是否存在，不存在需要新建各级目录
        if (!localDirFile.exists()) {
            localDirFile.mkdirs();
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("sftp文件服务器文件夹[{}],下载到本地目录[{}]", new Object[]{remoteFilePath, localDirFile});
        }
        ChannelSftp channelSftp = connect(sftpConfig);
        Vector<ChannelSftp.LsEntry> lsEntries = channelSftp.ls(remoteFilePath);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("远程目录下的文件为[{}]", lsEntries);
        }
        for (ChannelSftp.LsEntry entry : lsEntries) {
            String fileName = entry.getFilename();
            if (checkFileName(fileName)) {
                continue;
            }
            String remoteFileName = getRemoteFilePath(remoteFilePath, fileName);
            channelSftp.get(remoteFileName, localDirPath);
        }
        disConnect(channelSftp);
    }

    /**
     * 关闭流
     *
     * @param outputStream
     */
    private void closeStream(InputStream inputStream, OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkFileName(String fileName) {
        if (".".equals(fileName) || "..".equals(fileName)) {
            return true;
        }
        return false;
    }

    private String getRemoteFilePath(String remoteFilePath, String fileName) {
        if (remoteFilePath.endsWith("/")) {
            return remoteFilePath.concat(fileName);
        } else {
            return remoteFilePath.concat("/").concat(fileName);
        }
    }

    /**
     * 删除文件
     *
     * @param directory  要删除文件所在目录
     * @param deleteFile 要删除的文件
     * @param sftp
     */
    public void delete(String directory, String deleteFile, ChannelSftp sftp) {
        try {
            sftp.cd(directory);
            sftp.rm(deleteFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 列出目录下的文件
     *
     * @param directory  要列出的目录
     * @param sftpConfig
     * @return
     * @throws SftpException
     */
    public List<String> listFiles(String directory, SftpConfig sftpConfig) throws SftpException {
        ChannelSftp sftp = connect(sftpConfig);
        List<String> fileNameList = new ArrayList<String>();
        try {
            sftp.cd(directory);
        } catch (SftpException e) {
            return fileNameList;
        }
        Vector vector = sftp.ls(directory);
        for (int i = 0; i < vector.size(); i++) {
            if (vector.get(i) instanceof ChannelSftp.LsEntry) {
                ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) vector.get(i);
                String fileName = lsEntry.getFilename();
                if (".".equals(fileName) || "..".equals(fileName)) {
                    continue;
                }
                fileNameList.add(fileName);
            }
        }
        disConnect(sftp);
        return fileNameList;
    }

    /**
     * 断掉连接
     */
    public void disConnect(ChannelSftp sftp) {
        try {
            sftp.disconnect();
            sftp.getSession().disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 构造器
     */
    public SftpUtil(long count, long sleepTime) {
        this.count = count;
        this.sleepTime = sleepTime;
    }

    public SftpUtil() {

    }
}


