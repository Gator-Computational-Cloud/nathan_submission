package com.web.plan;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.util.SupplierUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.web.node.NodeADT;

public class Planner {
  private static HashMap<Integer, ArrayList<NodeADT>> hm = new HashMap<Integer, ArrayList<NodeADT>>();
  private static ArrayList<NodeADT> nadtl = new ArrayList<NodeADT>();
  private static HashMap<String, NodeADT> ledger = new HashMap<String, NodeADT>();
  private final String access_key;
  private final String secret_key;
  private final String token;

  public Planner(String access_key, String secret_key, String token) {
    // Constructor
    this.access_key = access_key;
    this.secret_key = secret_key;
    this.token = token;
  }

  public void parseXml(File f, String user, String wf_name) {
    try {

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(f);

      doc.getDocumentElement().normalize();

      NodeList nl = doc.getElementsByTagName("task");
      for (int i = 0; i < nl.getLength(); i++) {
        Node n = nl.item(i);
        if (n.getNodeType() == Node.ELEMENT_NODE) {
          Element e = (Element) n;

          String id = e.getElementsByTagName("id").item(0).getTextContent();
          ArrayList<String> deps = null;
          ArrayList<String> weights = null;

          if (!(e.getElementsByTagName("deps").item(0).getTextContent().equals(""))) {
            deps = new ArrayList<String>(Arrays.asList(e.getElementsByTagName("deps").item(0).getTextContent().split(",")));
          }

          String node_path = System.getProperty("user.dir") + "/USERS/" + user + "/workflows/" + wf_name + "/nodes/" + id + "/" + id + ".zip";
          NodeADT nadt = new NodeADT(id, node_path, deps, weights, access_key, secret_key, token, user, wf_name);
          nadtl.add(nadt);
          ledger.put(id, nadt);
        }
      }
    } catch (Exception x) {
      x.printStackTrace();
    }
  }

  public DirectedAcyclicGraph<NodeADT, DefaultEdge> plan() {
    DirectedAcyclicGraph<NodeADT, DefaultEdge> pl = new DirectedAcyclicGraph<>(null, SupplierUtil.createDefaultEdgeSupplier(), true);

    while(true) {
      ArrayList<NodeADT> lint = new ArrayList<NodeADT>();
      ArrayList<String> compIds = new ArrayList<String>();
      int max = 0;

      for (Integer key : hm.keySet()) {
        max = key;
        for (NodeADT nt : hm.get(key)) {
          lint.add(nt);
          compIds.add(nt.getId());
        }
      }

      if (lint.equals(nadtl)) {
        break;
      }

      for (NodeADT t : nadtl) {
        if (t.getDeps() == null) {
          if (hm.get(0) != null) {
            if (!(hm.get(0).contains(t))) {
              hm.get(0).add(t);
            }
          } else {
            ArrayList<NodeADT> temp = new ArrayList<NodeADT>();
            temp.add(t);
            hm.put(0, temp);
          }
        } else {
          if (!(lint.contains(t))) {
            if (compIds.containsAll(t.getDeps())) {
              if (hm.get((max + 1)) != null) {
                hm.get((max + 1)).add(t);
              } else {
                ArrayList<NodeADT> temp = new ArrayList<NodeADT>();
                temp.add(t);
                hm.put((max + 1), temp);
              }
            }
          }
        }
      }
    }

    try {
      ArrayList<NodeADT> nl;
      for (Integer key : hm.keySet()) {
        nl = hm.get(key);
        for (int i = 0; i < nl.size(); i++) {
          pl.addVertex(nl.get(i));
          if (nl.get(i).getDeps() != null) {
            for (int j = 0; j < nl.get(i).getDeps().size(); j++) {
              pl.addEdge(ledger.get(nl.get(i).getDeps().get(j)), nl.get(i));
            }
          }
        }
      }
    } catch (Exception a) {
      a.printStackTrace();
    }

    return pl;
  }

  public HashMap<Integer, ArrayList<NodeADT>> getHmap() {
    return hm;
  }

  public ArrayList<NodeADT> getNadtl() {
    return nadtl;
  }

  public HashMap<String, NodeADT> getLedger() {
    return ledger;
  }
}
