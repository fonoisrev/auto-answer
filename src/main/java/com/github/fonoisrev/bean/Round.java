package com.github.fonoisrev.bean;

public class Round {
    
    public boolean lock;
    
    public int roundId;
    
    public String roundName;
    
    public int starNum;
    
    public int userStarNum;
    
    @Override
    public String toString() {
        return "Round{" +
               "lock=" + lock +
               ", roundId=" + roundId +
               ", roundName='" + roundName + '\'' +
               ", starNum=" + starNum +
               ", userStarNum=" + userStarNum +
               '}';
    }
}
