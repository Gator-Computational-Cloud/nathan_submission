package com.web.exec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

import com.web.node.NodeADT;

public class PlanExecutor {
  public PlanExecutor() {}

  public void executePlan(DirectedAcyclicGraph<NodeADT, DefaultEdge> pl, HashMap<Integer, ArrayList<NodeADT>> hm) {
    ExecutorService executor = Executors.newFixedThreadPool(pl.vertexSet().size());
    int done_ct;

    try {
      for (ArrayList<NodeADT> nl : hm.values()) {
        done_ct = 0;
        for (int i = 0; i < nl.size(); i++) {
          ArrayList<NodeADT> children = new ArrayList<NodeADT>();
          ArrayList<NodeADT> parents = new ArrayList<NodeADT>();

          for (DefaultEdge e : pl.outgoingEdgesOf(nl.get(i))) {
            children.add(pl.getEdgeTarget(e));
          }
          for (DefaultEdge e : pl.incomingEdgesOf(nl.get(i))) {
            parents.add(pl.getEdgeSource(e));
          }

          if (pl.incomingEdgesOf(nl.get(i)).size() == 0) {
            nl.get(i).ei.setFilesReceived(true);
          }

          nl.get(i).setChildren(children);
          nl.get(i).setParents(parents);
          executor.execute(nl.get(i));
        }
        while (true) {
          if (done_ct == nl.size()) {
            break;
          } else {
            done_ct = 0;
            for (int i = 0; i < nl.size(); i++) {
              if (nl.get(i).ei.getExecStatus() == true) {
                done_ct += 1;
              }
            }
          }
          Thread.sleep(500);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    executor.shutdown();
  }
}
