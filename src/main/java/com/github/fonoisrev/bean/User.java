package com.github.fonoisrev.bean;

public class User {
    
    public String userName;
    
    public String token;
    
    /*信息技术公司*/
    public String corporationName;
    
    public int userId;
    
    @Override
    public String toString() {
        return "User{" +
               "name='" + userName + '\'' +
               ", id=" + userId +
               '}';
    }
}
