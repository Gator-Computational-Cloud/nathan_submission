package com.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

import com.web.aws.AwsProvisioner;
import com.web.exec.PlanExecutor;
import com.web.node.NodeADT;
import com.web.plan.Planner;

public class ExecDriver {
  public static void main(String[] args) {
    /**
    Code to execute workflows in the cloud
    **/
    final String user;
    final String wf_name;
    final String wf_path;
    final String pem_path;
    final String access_key;
    final String secret_key;
    final String pem_mat;
    final String token;
    
    if (args.length != 5) {
      System.exit(-1);
    } else {
      // ====================================================================
      try {
        // ==================================================================
        user = args[0];
        wf_name = args[1];
        access_key = args[2];
        secret_key = args[3];
        token = args[4];

        wf_path = System.getProperty("user.dir") + "/USERS/" + user + "/workflows/" + wf_name + "/" + wf_name + ".xml";
        pem_path = System.getProperty("user.dir") + "/USERS/" + user + "/executions/" + wf_name + "/pem/" + wf_name + ".pem";

        File wf = new File(wf_path);
        File pem = new File(pem_path);

        if (wf.exists()) {
          Planner pr = new Planner(access_key, secret_key, token);
          pr.parseXml(wf, user, wf_name);

          ArrayList<NodeADT> nadtl = pr.getNadtl();
          HashMap<String, NodeADT> ledger = pr.getLedger();

          AwsProvisioner aws = new AwsProvisioner(access_key, secret_key, token);
          pem_mat = aws.createKeyPair(wf_name);
          aws.createSecurityGroup(wf_name);

          if (pem.createNewFile()) {
            System.out.println("file created : " + pem.getName());
          }

          BufferedWriter bw = new BufferedWriter(new FileWriter(pem, false));
          bw.write(pem_mat);
          bw.close();

          for (NodeADT n : nadtl) {
            n.ei.setPemPath(pem_path);
            n.ei.setKpName(wf_name);
            n.ei.setSgName(wf_name);
            n.setLedger(ledger);
          }

          DirectedAcyclicGraph<NodeADT, DefaultEdge> pl = pr.plan();
          HashMap<Integer, ArrayList<NodeADT>> hm = pr.getHmap();

          PlanExecutor pex = new PlanExecutor();
          pex.executePlan(pl, hm);
          aws.deleteKeyPair(wf_name);
          aws.deleteSecurityGroup(wf_name);
        }
        // ==================================================================
      } catch (Exception e) {
        e.printStackTrace();
      }
      // ======================================================================
    }
  }
}
