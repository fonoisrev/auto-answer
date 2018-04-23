package com.github.fonoisrev.bean;

public class Option {
    
    // a, b, c, d
    public String id;
    
    public String optionContentStr;
    
    public int optionResultInt;
    
    @Override
    public String toString() {
        return "Option{" +
               "id='" + id + '\'' +
               ", optionContentStr='" + optionContentStr + '\'' +
               ", optionResultInt=" + optionResultInt +
               '}';
    }
}
