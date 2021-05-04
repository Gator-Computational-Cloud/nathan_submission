package com.web.jsch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
*
* @author Professor Mohan
*
* This code is used to handle uploads, downloads, and executions on a virtual
* machine using the jsch library
*
**/

public class JschProvisioner {
  public JschProvisioner() {
    // Constructor
  }

  public void download(String src, String dest, String host, String user, String pem, int port) throws SftpException {
    try {
      JSch jsch = new JSch();
      Session session = null;
      Channel channel = null;
      ChannelSftp channelSftp = null;
      jsch.addIdentity(pem);
      session = jsch.getSession(user, host, port);

      java.util.Properties config = new java.util.Properties();
      config.put("StrictHostKeyChecking", "no");

      session.setConfig(config);
      session.setPort(port);
      session.connect();

      channel = session.openChannel("sftp");
      channel.connect();
      channelSftp = (ChannelSftp) channel;
      channelSftp.get(src,dest);
      channel.disconnect();
      session.disconnect();
    } catch (Exception e) {
      System.out.println("Error at : " + e.toString());
    }
  }

  public void upload(String src, String dest, String host, String user, String pem, int port) {
    try {
      JSch jsch = new JSch();
      Session session = null;
      Channel channel = null;
      ChannelSftp channelSftp = null;
      jsch.addIdentity(pem);
      session = jsch.getSession(user, host, port);
      java.util.Properties config = new java.util.Properties();
      config.put("StrictHostKeyChecking", "no");
      session.setConfig(config);
      session.setPort(port);
      session.connect();
      channel = session.openChannel("sftp");
      channel.connect();
      channelSftp = (ChannelSftp) channel;
      channelSftp.cd(dest);
      File file = new File(src);
      if(file.isDirectory()){
        String currentDirectory=channelSftp.pwd();
        SftpATTRS attrs = null;
        try {
          attrs = channelSftp.stat(currentDirectory+"/" + file.getName());
        } catch (Exception e) {

        }
        if (attrs != null) {
          recursiveFolderDelete(channelSftp, file.getName());
        }
        channelSftp.mkdir(file.getName());
        System.out.println("#created folder: " + file.getName() + " in " + dest);
        dest = dest + "/" + file.getName();
        for(File item: file.listFiles()){
          upload(item.getAbsolutePath(), dest, host, user, pem, port);
        }
        channelSftp.cd(dest.substring(0, dest.lastIndexOf('/')));

      }
      else{
        channelSftp.put(new FileInputStream(file), file.getName());
      }
      channel.disconnect();
      session.disconnect();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public String execute(String host, String user, String pem, int port, ArrayList<String> commands){
    String strLogMessages = "";
    try {
      JSch jsch = new JSch();
      jsch.addIdentity(pem);
      Session session = jsch.getSession(user, host, port);
      java.util.Properties config = new java.util.Properties();
      config.put("StrictHostKeyChecking", "no");
      session.setConfig(config);
      session.connect();
      Channel channel = session.openChannel("shell");//only shell
      OutputStream inputstream_for_the_channel = channel.getOutputStream();
      PrintStream shellStream = new PrintStream(inputstream_for_the_channel, true);
      channel.connect();
      for(String command: commands) {
        shellStream.println(command);
        shellStream.flush();
      }
      shellStream.close();
      InputStream outputstream_from_the_channel = channel.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(outputstream_from_the_channel));
      String line;
      while ((line = br.readLine()) != null){
        strLogMessages = strLogMessages + line+"\n";
      }
      do {
      } while(!channel.isEOF());
      outputstream_from_the_channel.close();
      br.close();
      session.disconnect();
      channel.disconnect();
      return strLogMessages;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return strLogMessages;
  }

  @SuppressWarnings("unchecked")
  private static void recursiveFolderDelete(ChannelSftp channelSftp, String path) throws SftpException {
    Collection<ChannelSftp.LsEntry> list = channelSftp.ls(path);
    System.out.println("#deleting : " + path);
    for (ChannelSftp.LsEntry item : list) {
      if (!item.getAttrs().isDir()) {
        channelSftp.rm(path + "/" + item.getFilename());
      } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
        try {
          channelSftp.rmdir(path + "/" + item.getFilename());
        } catch (Exception e) {
          recursiveFolderDelete(channelSftp, path + "/" + item.getFilename());
        }
      }
    }
    channelSftp.rmdir(path);
  }
}
