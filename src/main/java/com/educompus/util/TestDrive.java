package com.educompus.util;
import com.educompus.service.GoogleDriveService;

public class TestDrive {
    public static void main(String[] args) {
        try {
            new GoogleDriveService();
            System.out.println("SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
