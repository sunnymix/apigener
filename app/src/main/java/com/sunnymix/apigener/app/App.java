package com.sunnymix.apigener.app;

import com.sunnymix.apigener.annotation.Data;

/**
 * @author Sunny
 */
public class App {

    @Data
    public static class Project {
        public String name;
    }

    public static void main(String[] args) {
        Project project = new Project();
        project.setName("APIGENER");
        String projectInfo = "Project.name: " + project.getName();
        System.out.println(projectInfo);
    }

}
