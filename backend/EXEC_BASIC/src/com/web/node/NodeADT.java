package com.web.node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.amazonaws.services.ec2.model.Instance;
import com.web.aws.AwsProvisioner;
import com.web.exec.ExecInfo;
import com.web.jsch.JschProvisioner;

public class NodeADT implements Runnable {
  private final String id;
  private final String wf_name;
  private final String user;
  private final String path;
  private final ArrayList<String> deps;
  private final ArrayList<String> weights;
  private final String access_key;
  private final String secret_key;
  private final String token;

  private ArrayList<NodeADT> children;
  private ArrayList<NodeADT> parents;
  private HashMap<String, NodeADT> ledger;

  public ExecInfo ei = new ExecInfo();

  public NodeADT(String id, String path, ArrayList<String> deps, ArrayList<String> weights, String access_key, String secret_key, String token, String user, String wf_name) {
    // Constructor
    this.id = id;
    this.path = path;
    this.deps = deps;
    this.weights = weights;
    this.access_key = access_key;
    this.secret_key = secret_key;
    this.token = token;
    this.user = user;
    this.wf_name = wf_name;
  }

  public String getId() {
    return id;
  }

  public String getPath() {
    return path;
  }

  public ArrayList<String> getDeps() {
    return deps;
  }

  public ArrayList<String> getWeights() {
    return weights;
  }

  public void setLedger(HashMap<String, NodeADT> ledger) {
    this.ledger = ledger;
  }
  
  public HashMap<String, NodeADT> getLedger() {
	  return ledger;
  }

  public void setChildren(ArrayList<NodeADT> children) {
    this.children = children;
  }

  public ArrayList<NodeADT> getChildren() {
    return children;
  }

  public void setParents(ArrayList<NodeADT> parents) {
    this.parents = parents;
  }

  public ArrayList<NodeADT> getParents() {
      return parents;
  }
  
  public void run() {
    System.out.println("executing : " + id);

    String ins_id;
    AwsProvisioner aws;
    JschProvisioner jsch;
    ArrayList<String> cmds;
    String logs;

    Instance ins = null;

    try {
      aws = new AwsProvisioner(access_key, secret_key, token);
      jsch = new JschProvisioner();

      if (ei.getInitialized() == false) {
        ins_id = aws.createInstances(1, "t2.micro", "ami-0885b1f6bd170450c", ei.getKpName(), ei.getSgName());
        ei.setInsId(ins_id);
        Thread.sleep(1500);
        while (true) {
          if (ei.getInitialized() == true) {
            break;
          } else {
            ins = aws.describeInstance(ei.getInsId());
            if (ins.getState().getCode() == 16) {
              ei.setIp(ins.getPublicIpAddress());
              ei.setInitialized(true);
            }
          }
          Thread.sleep(2500);
        }
      }
      Thread.sleep(25000);
      // UPLOAD NODE SRC TO AWS INSTANCE
      System.out.println("uploading node src to " + ei.getIp());
      jsch.upload(path, "/home/ubuntu", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
      jsch.upload(ei.getPemPath(), "/home/ubuntu", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
      System.out.println("upload complete");
      Thread.sleep(2500);
      // INSTALL JAVA AND UNZIP .ZIP FILE
      System.out.println("initializing vm and unzipping src");
      cmds = new ArrayList<String>();
      cmds.add("sudo apt update -qq");
      cmds.add("sudo apt update -qq");
      cmds.add("echo Y | sudo apt install default-jre -qq");
      cmds.add("echo Y | sudo apt install default-jdk -qq");
      cmds.add("echo Y | sudo apt install unzip -qq");
      cmds.add("unzip " + id + ".zip");
      cmds.add("ls");
      cmds.add("exit");
      logs = jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
      System.out.println("initialization and unzipping complete");
      cmds.clear();

      if (ei.getFilesReceived() == false) {
        for (int i = 0; i < parents.size(); i++) {
          System.out.println("transferring files to " + id + " from " + parents.get(i).getId());

          // TRANSFER INPUT FILES
          cmds.add("chmod 600 " + wf_name + ".pem");
          cmds.add("cd " + id);
          cmds.add("cd data/in");
          cmds.add("scp -i ../../../" + wf_name + ".pem -o StrictHostKeyChecking=no ubuntu@" + parents.get(i).ei.getIp() + ":/home/ubuntu/" + parents.get(i).getId() + "/data/out/* .");
          cmds.add("exit");
          logs += jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
          cmds.clear();

          parents.get(i).ei.setTransferCount((parents.get(i).ei.getTransferCount() + 1));
          if (parents.get(i).ei.getTransferCount() == parents.get(i).getChildren().size()) {
            System.out.println("transfers complete - terminating " + parents.get(i).getId());
            aws.terminateInstance(parents.get(i).ei.getInsId());
          }
        }
        ei.setFilesReceived(true);
      }

      // EXECUTE NODE
      System.out.println("executing code on " + id);
      cmds.add("cd " + id);
      cmds.add("ls data/in");
      cmds.add("chmod +x ./build.sh");
      cmds.add("chmod +x ./run.sh");
      cmds.add("./build.sh");
      cmds.add("./run.sh");
      cmds.add("exit");
      logs += jsch.execute(ei.getIp(), "ubuntu", ei.getPemPath(), 22, cmds);
      System.out.println("done executing code on " + id);
      cmds.clear();

      if (children.size() == 0) {
        System.out.println("downloading results from " + id);
        jsch.download("/home/ubuntu/" + id + "/data/out/*", System.getProperty("user.dir") + "/USERS/" + user + "/executions/" + wf_name + "/results", ei.getIp(), "ubuntu", ei.getPemPath(), 22);
        System.out.println("download complete");
        System.out.println("terminating " + id);
        aws.terminateInstance(ei.getInsId());
        while(true) {
          ins = aws.describeInstance(ei.getInsId());
          if (ins.getState().getCode() == 48) {
            break;
          }
          Thread.sleep(2500);
        }
        Thread.sleep(5000);
      }

      File log_file = new File(System.getProperty("user.dir") + "/USERS/" + user + "/executions/" + wf_name + "/logs/" + id + "_logs.txt");

      if (log_file.createNewFile()) {
        System.out.println("file created : " + log_file.getName());
      }

      BufferedWriter bw = new BufferedWriter(new FileWriter(log_file, false));
      bw.write(logs);
      bw.close();

      System.out.println("done with : " + id);
      ei.setExecStatus(true); // !IMPORTANT!
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
