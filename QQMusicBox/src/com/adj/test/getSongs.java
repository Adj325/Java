package com.adj.test;

import java.util.Map;
import com.adj.service.impl.musicSingletonImpl;
public class getSongs {
    public static void main(String[] args) {
        Map<String,Object>[] result = musicSingletonImpl.getInstance().getSongs("青花瓷");
        System.out.println(result);
    }
}
