package my.impl;

import cn.edu.sustech.cs307.config.Config;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.factory.ServiceFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Boolean.*;

// 此文档用来处理先修课，将先修课逻辑表达转换为所有满足先修课课程组合
// 如果是null 返回null
// 如果是course 返回course
// 其他情况 返回or

public class PrerequisiteService {
    protected static final ServiceFactory SERVICE_FACTORY= Config.getServiceFactory();

    private static HashMap<Prerequisite,TreeNode> link = new HashMap<>(); // link between treeNods and Prerequisite structure

    public static void main(String[] args) {
        CoursePrerequisite A = new CoursePrerequisite("A");
        CoursePrerequisite B = new CoursePrerequisite("B");
        CoursePrerequisite C = new CoursePrerequisite("C");
        CoursePrerequisite D = new CoursePrerequisite("D");
        List<Prerequisite> AB = new ArrayList<>(){{add(A);add(B);}};
        List<Prerequisite> CD = new ArrayList<>(){{add(C);add(D);}};

        // test 1 : (A||B)&C
        OrPrerequisite test1_or = new OrPrerequisite(AB);
        List<Prerequisite> test1_list = new ArrayList<>(){{add(test1_or);add(C);}};
        Prerequisite test1 = new AndPrerequisite(test1_list);

        // test 2 : (A&B)||C
        AndPrerequisite test2_and = new AndPrerequisite(AB);
        List<Prerequisite> test2_list = new ArrayList<>(){{add(test2_and);add(C);}};
        Prerequisite test2 = new OrPrerequisite(test2_list);

        // test 3 : (A||B) & (C||D)
        OrPrerequisite test3_or = new OrPrerequisite(CD);
        List<Prerequisite> test3_list = new ArrayList<>(){{add(test1_or);add(test3_or);}};
        Prerequisite test3 = new AndPrerequisite(test3_list);

        // testing
//        System.out.println("A");
//        System.out.println(findAllSatisfied(A));
//        System.out.println("A||B");
//        System.out.println(findAllSatisfied(test1_or));
//        System.out.println("A&B");
//        System.out.println(findAllSatisfied(test2_and));
//        System.out.println("(A||B)&C");
//        System.out.println(findAllSatisfied(test1));
//        System.out.println("(A&B)||C");
//        System.out.println(findAllSatisfied(test2));
        System.out.println("(A||B) & (C||D)");
        System.out.println(findAllSatisfied(test3));


    }


    public static Prerequisite findAllSatisfied(Prerequisite prerequisite){
        if(prerequisite == null || prerequisite instanceof CoursePrerequisite){
            return prerequisite;
        }

        link = new HashMap<>();
        TreeNode tn = new TreeNode(prerequisite);
        List<CoursePrerequisite> cpl = new ArrayList<>();
        for(Prerequisite key : link.keySet()){
            if(key instanceof CoursePrerequisite ){
                if(!cpl.contains(key)){
                    cpl.add((CoursePrerequisite) key);
                }
            }
        }

        //////// test all possibility
        /// 不考虑prerequisite 为 null的情况
        int numOfCourse = cpl.size();
        int numOfTest = (int) Math.pow(2,numOfCourse);

        /// test getBooleanValue
        // set boolean value

//        ArrayList<Integer> xs = new ArrayList<>(numOfCourse);
        ArrayList<Prerequisite> all_satisfied = new ArrayList<>();
        // 0 does not need to be test
        for (int i = 1; i < numOfTest; i++) {
            List<Prerequisite> cp_testing = new ArrayList<>();
//            System.out.println("-----------------------------");
//            System.out.println(i);
            String s = Integer.toBinaryString(i);
            String[] ss = s.split("");
            int numOfUnTest = numOfCourse- ss.length;
//            if(!xs.isEmpty()){
//                xs.clear();
//            }
            for (int j = 0; j < numOfCourse; j++) {
                TreeNode temp = link.get(cpl.get(j));
                if(j<numOfUnTest){
                    temp.booleanValue = FALSE;
//                    xs.add(0);
                }else {
                    int x = Integer.parseInt(ss[j-numOfUnTest]);
                    temp.booleanValue =( x != 0);
//                    xs.add(x);
                    if(x!=0){
                        cp_testing.add(cpl.get(j));
                    }
                }
            }
            if(tn.getBooleanValue()){
                if(cp_testing.size() ==1){
                    all_satisfied.add(cp_testing.get(0));
                }else{
                    all_satisfied.add(new AndPrerequisite(cp_testing));
                }
            }
//            System.out.println("-----------------------------");
         }
//

        return new OrPrerequisite(all_satisfied);
    }

    private static class TreeNode{
        List<TreeNode> nodes; // child nodes
        Prerequisite TypeOfThisNode;
        boolean booleanValue ;

        // construct
        public TreeNode(Prerequisite p){
            nodes = new ArrayList<>();
            TypeOfThisNode = p;
            if(p instanceof CoursePrerequisite){
                CoursePrerequisite c = (CoursePrerequisite) p;
                link.put(c,this);
            }else if(p instanceof OrPrerequisite){
                OrPrerequisite o = (OrPrerequisite) p;
                link.put(o,this);
                for(Prerequisite n :((OrPrerequisite) TypeOfThisNode).terms) {
                    TreeNode child = new TreeNode(n);
                    nodes.add(child);
                }
            }else {
                AndPrerequisite a = (AndPrerequisite) p;
                link.put(a,this);
                for(Prerequisite n :((AndPrerequisite) TypeOfThisNode).terms) {
                    TreeNode child = new TreeNode(n);
                    nodes.add(child);
                }
            }
        }

        // getBooleanValue
        public boolean getBooleanValue(){
            boolean here;
            if(TypeOfThisNode instanceof CoursePrerequisite){
                here = booleanValue;
                return here;
            }else if(TypeOfThisNode instanceof OrPrerequisite){
                boolean temp = FALSE;
                for(TreeNode n : nodes){
                    temp = n.getBooleanValue() | temp;
                }
                return temp;
            }else {
                boolean temp = TRUE;
                for(TreeNode n : nodes){
                    temp = n.getBooleanValue() & temp;
                }
                return temp;
            }
        }


        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if(TypeOfThisNode instanceof CoursePrerequisite){
                sb.append(((CoursePrerequisite) TypeOfThisNode).courseID);
            }else if(TypeOfThisNode instanceof OrPrerequisite){
                sb.append("(");
                for(TreeNode n : nodes){
                    sb.append(n).append(" OR ");
                }
                sb.append(")");
            }else {
                sb.append("(");
                for(TreeNode n : nodes){
                    sb.append(n).append(" AND ");
                }
                sb.append(")");
            }

            return sb.toString();

        }
    }


}