package com.github.fonoisrev.bean;

import java.util.ArrayList;
import java.util.Collection;

public class Question {

    public int id;
    
    public String title;
    
    public int verifyCode;
    
    public Collection<Option> options = new ArrayList<>();
}
