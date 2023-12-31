package test.Util;

import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;

import java.util.*;

public class prerequisiteTool extends Util{
    private static HashMap<Prerequisite,BooleanTreeNode> correspondMap;

    private prerequisiteTool(){
        super();
    }

    /*public static void main(String[] args) {
        CoursePrerequisite ca=new CoursePrerequisite("A");
        CoursePrerequisite cb=new CoursePrerequisite("B");
        CoursePrerequisite cc=new CoursePrerequisite("C");

        AndPrerequisite and2=new AndPrerequisite(Arrays.asList(ca,cb));
        OrPrerequisite or1=new OrPrerequisite(Arrays.asList(and2,cc));



        System.out.println(getExpression(or1));
        System.out.println(getExpression(advancePrerequisite(or1)));

    }*/

    //用于获取 已简化的prerequisite 的小组数（单个课程算一组，同一OrPrerequisite下的所有课程也算一组）
    public static int getGroupCount(Prerequisite simplifiedRoot){
        if(simplifiedRoot==null){
            return 0;
        }
        else if(simplifiedRoot instanceof CoursePrerequisite){
            return 1;
        }else if(simplifiedRoot instanceof AndPrerequisite){
            AndPrerequisite and=(AndPrerequisite) simplifiedRoot;
            return and.terms.size();
        }else if(simplifiedRoot instanceof OrPrerequisite){
            OrPrerequisite or=(OrPrerequisite) simplifiedRoot;
            return or.terms.size();
        }else {
            return 0;
        }
    }

    //用于判断 输入的prerequisite是否已经被简化至两层结构
    private static boolean isAdvanced(Prerequisite p){
        if(p==null||p instanceof CoursePrerequisite){
            return true;
        }else if(p instanceof AndPrerequisite){
            AndPrerequisite and=(AndPrerequisite)p;
            for (Prerequisite child: and.terms){
                if(!(child instanceof CoursePrerequisite)){
                    return false;
                }
            }
            return true;
        }else if(p instanceof OrPrerequisite){
            OrPrerequisite or=(OrPrerequisite) p;
            for (Prerequisite child:or.terms){
                if(!(child instanceof CoursePrerequisite)){
                    return false;
                }
            }

            return true;
        }else {
            return false;
        }
    }

    //把p用字符串的形式表现出来
    /*private static String getExpression(Prerequisite p){
        return p.when(new Prerequisite.Cases<>() {
            @Override
            public String match(AndPrerequisite self) {
                String[] children = self.terms.stream()
                        .map(term -> term.when(this))
                        .toArray(String[]::new);
                return '(' + String.join(" AND ", children) + ')';
            }

            @Override
            public String match(OrPrerequisite self) {
                String[] children = self.terms.stream()
                        .map(term -> term.when(this))
                        .toArray(String[]::new);
                return '(' + String.join(" OR ", children) + ')';
            }

            @Override
            public String match(CoursePrerequisite self) {
                return self.courseID;
            }
        });
    }*/

    //courseService所用到的方法，简化prerequisite
    public static Prerequisite advancePrerequisite(Prerequisite rawPrerequisite){
        List<CoursePrerequisite> allCourseList= getCoursePrerequisiteList(rawPrerequisite);

        //给每个correspondMap中的布尔树节点建树
        BooleanTreeNode.buildBooleanTree();

        return getFinalTree(allCourseList,rawPrerequisite);
    }

    private static Prerequisite getFinalTree(List<CoursePrerequisite> allCourseList,Prerequisite root){
        if(isAdvanced(root)){
            return root;
        }

        Prerequisite treeRoot=null;


        int maxSize =(int)(Math.pow(2, allCourseList.size()));

        List<Prerequisite> orTerms =new ArrayList<>();

        //构建一个内或外和的OrTerms
        for (int i = 0; i < maxSize; i++) {
            char[]bitStr=toBinary(i, allCourseList.size()).toCharArray();

            //System.out.println(bitStr);

            for (int bit=0;bit<bitStr.length;bit++){
                BooleanTreeNode btn=correspondMap.get(allCourseList.get(bit));
                btn.boolValue=(bitStr[bit]=='1');
            }
            //System.out.println(bitStr);

            //System.out.println(correspondMap.get(root).getBoolValue());


            if(correspondMap.get(root).getBoolValue()){
                List<Prerequisite>ts=new ArrayList<>();

                for (CoursePrerequisite cp:allCourseList){
                    BooleanTreeNode btn=correspondMap.get(cp);
                    if(btn.boolValue){
                        ts.add(btn.correspond);
                    }
                }

                orTerms.add(new AndPrerequisite(ts));
            }

        }

        treeRoot=new OrPrerequisite(orTerms);

        return treeRoot;
    }

    //十进制转二进制，后续会用1表示true，0表示false
    private static String toBinary(int number,int digits){
        String cover = Integer.toBinaryString(1 << digits).substring(1);
        String s = Integer.toBinaryString(number);

        //System.out.println(number+" "+s+" "+digits+" "+cover);

        return s.length() < digits ? cover.substring(s.length()) + s : s;
    }

    //获取一门课的先修课集中关联的所有课程
    //并给他们每一个建立一个BooleanTreeNode，并放进correspondMap
    private static List<CoursePrerequisite> getCoursePrerequisiteList(Prerequisite prerequisiteRoot){
        correspondMap=new HashMap<>();

        List<CoursePrerequisite> coursePrerequisiteList =new LinkedList<>();
        Queue<Prerequisite> prerequisiteQueue=new LinkedList<>();
        prerequisiteQueue.offer(prerequisiteRoot);

        //BFS
        while (!prerequisiteQueue.isEmpty()){
            Prerequisite poll=prerequisiteQueue.poll();

            BooleanTreeNode btn=new BooleanTreeNode(poll);

            if(poll instanceof AndPrerequisite){
                AndPrerequisite and=(AndPrerequisite) poll;
                for (Prerequisite child:and.terms){
                    prerequisiteQueue.offer(child);
                }
            }else if(poll instanceof OrPrerequisite) {
                OrPrerequisite or = (OrPrerequisite) poll;
                for (Prerequisite child : or.terms) {
                    prerequisiteQueue.offer(child);
                }
            }else if(poll instanceof CoursePrerequisite){
                CoursePrerequisite course=(CoursePrerequisite) poll;
                coursePrerequisiteList.add(course);
            }
        }
        return coursePrerequisiteList;
    }

    private static class BooleanTreeNode{
        List<BooleanTreeNode>children;
        boolean boolValue;
        Prerequisite correspond;

        public BooleanTreeNode(Prerequisite p){
            this.children=new ArrayList<>();
            correspond=p;
            if(p instanceof CoursePrerequisite){
                CoursePrerequisite cp=(CoursePrerequisite) p;
                correspondMap.put(cp,this);
            }else if(p instanceof OrPrerequisite){
                OrPrerequisite or=(OrPrerequisite)p;
                correspondMap.put(or,this);
            }else if(p instanceof AndPrerequisite){
                AndPrerequisite and=(AndPrerequisite) p;
                correspondMap.put(and,this);
            }
        }

        public boolean getBoolValue() {
            if (correspond instanceof CoursePrerequisite) {
                return boolValue;
            } else if (correspond instanceof OrPrerequisite) {
                boolean value = false;
                //递归，当前节点为or时，若其中有一个为true，则该节点为true
                for (BooleanTreeNode child : children) {
                    value = child.getBoolValue() || value;
                }
                this.boolValue = value;
                return this.boolValue;
            } else if (correspond instanceof AndPrerequisite) {
                boolean value = true;
                //递归，当前节点为and时，若其中有一个为false，则该节点为false
                for (BooleanTreeNode child : children) {
                    value = child.getBoolValue() && value;
                }
                this.boolValue = value;
                return this.boolValue;
            } else {
                return false;
            }
        }

        private static void buildBooleanTree(){
            for (Prerequisite p:correspondMap.keySet()){
                BooleanTreeNode btn=correspondMap.get(p);
                if(p instanceof AndPrerequisite){
                    AndPrerequisite and=(AndPrerequisite) p;
                    for (Prerequisite child:and.terms){
                        btn.children.add(correspondMap.get(child));
                    }
                }else if(p instanceof OrPrerequisite){
                    OrPrerequisite or =(OrPrerequisite) p;
                    for (Prerequisite child: or.terms){
                        btn.children.add(correspondMap.get(child));
                    }
                }
            }
        }


    }
}