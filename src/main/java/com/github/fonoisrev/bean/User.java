package com.github.fonoisrev.bean;

import lombok.ToString;

@ToString(exclude = {"token"})
public class User {
    
    public String userName;
    
    public String token;
    
    public int userId;
}
