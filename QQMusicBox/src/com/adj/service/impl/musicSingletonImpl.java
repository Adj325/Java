package com.adj.service.impl;
import java.util.*;
import com.adj.service.music;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;

public class musicSingletonImpl implements music{
    // 单例模式
    private static class SingeltonHandler{
        private static musicSingletonImpl INSTANCE = new musicSingletonImpl();
    }
    public static musicSingletonImpl getInstance(){
        return SingeltonHandler.INSTANCE;
    }

    // 检测下载链接进程
    class StatusCodeThread extends Thread{
        public StatusCodeThread(Map<String,Object> song, String[] urlArr) {
            run(song, urlArr);
        }
        public void run(Map<String,Object> song, String[] urlArr){
            if(getStatusCode(urlArr[0]).equals("200"))
                song.put("flac", urlArr[0]);
            if(getStatusCode(urlArr[1]).equals("200"))
                song.put("ape", urlArr[1]);
            if(getStatusCode(urlArr[2]).equals("200"))
                song.put("mp3320", urlArr[2]);
            if(getStatusCode(urlArr[3]).equals("200"))
                song.put("mp3128", urlArr[3]);
        }
    }

    // 获取用于下载音乐的vkey
    public String getVkey(String guid) {
        try {
            String url = "http://base.music.qq.com/fcgi-bin/fcg_musicexpress.fcg?json=3&guid="+guid;
            // 创建请求Get实例
            HttpGet httpGet = new HttpGet(url);

            // 设置头部信息
            httpGet.setHeader("Host", "base.music.qq.com");
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87");

            CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
            CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(httpGet);
            // 根据utf-8解码
            String entity = EntityUtils.toString (closeableHttpResponse.getEntity(), "utf-8");
            // 提取json信息
            String json = entity.substring(13, entity.length()-2);
            JSONObject jsonObject = JSONObject.fromObject(json);
            // 关闭
            closeableHttpClient.close();
            return jsonObject.getString("key");
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    // 检测链接是否有效
    public String getStatusCode(String url){
        try{
            url = url.replace("/", "%2F").replace("&", "%3F").replace("=", "%3D");
            String testUrl = "http://pl.soshoulu.com/ajax/shoulu.ashx?_type=webspeed&px=1&url="+url;
            HttpGet httpGet = new HttpGet(testUrl);
            httpGet.setHeader("Host", "pl.soshoulu.com");
            httpGet.setHeader("Referer", "http://pl.soshoulu.com/webspeed.aspx");
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87");
            CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
            CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(httpGet);
            String entity = EntityUtils.toString (closeableHttpResponse.getEntity(), "utf-8");
            String status_code = entity.substring(0, 3);
            // 关闭
            closeableHttpClient.close();
            return status_code;
        }
        catch (Exception e){
            return "";
        }
    }

    // 根据key, 搜索歌曲
    public Map<String,Object>[] getSongs(String key){
        try {
            System.out.println("搜索: "+key);
            String url = "http://soso.music.qq.com/fcgi-bin/search_cp?aggr=0&catZhida=0&lossless=1&sem=1&n=15&t=0&p=1&remoteplace=wo.shi.nidaye&g_tk=5381&loginUin=0&hostUin=0&format=json&inCharset=GB2312&outCharset=utf-8&notice=0&platform=yqq&needNewCode=0&w=";

            // 创建请求Get实例
            HttpGet httpGet = new HttpGet(url+key);

            // 设置头部信息
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87");
            httpGet.setHeader("Host", "soso.music.qq.com");
            // 创建客户端
            CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
            // 客户端执行httpGet方法，返回响应
            CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(httpGet);

            String entity = EntityUtils.toString (closeableHttpResponse.getEntity(), "utf-8");
            JSONObject jsonObject = JSONObject.fromObject(entity);
            JSONArray list = jsonObject.getJSONObject("data").getJSONObject("song").getJSONArray("list");
            // 歌手, 歌名, 专辑, mid, guid, vkey
            Map<String,Object>[] result = new Map[list.size()];
            // 链接检测进程
            StatusCodeThread[] threads = new StatusCodeThread[list.size()];
            System.out.println("长度: "+list.size());
            for(int i = 0; i < list.size(); i++) {
                result[i] =  new HashMap<>();;
                JSONObject song = (JSONObject)list.get(i);
                String guid = song.getString("docid");
                String songmid = song.getString("songmid");
                String vKey = getVkey(guid);
                String flac_url = "http://ws.stream.qqmusic.qq.com/F000"  +songmid+".flac?vkey="+vKey+"&guid="+guid+"&fromtag=53";
                String ape_url = "http://ws.stream.qqmusic.qq.com/A000"   +songmid+".ape?vkey=" +vKey+"&guid="+guid+"&fromtag=53";
                String mp3128_url = "http://ws.stream.qqmusic.qq.com/M500"+songmid+".mp3?vkey=" +vKey+"&guid="+guid+"&fromtag=53";
                String mp3320_url = "http://ws.stream.qqmusic.qq.com/M800"+songmid+".mp3?vkey=" +vKey+"&guid="+guid+"&fromtag=53";

                String singer = ((JSONObject)song.getJSONArray("singer").get(0)).getString("name");

                String songname = song.getString("songname");
                String albumname = song.getString("albumname");
                // 下载链接
                String[] urlArr = {flac_url, ape_url, mp3320_url, mp3128_url};
                result[i].put("songname", songname);
                result[i].put("singer", singer);
                result[i].put("albumname", albumname);
                // 检测下载链接
                threads[i] = new StatusCodeThread(result[i], urlArr);
                threads[i].start();
            }
            // 等待所有进程都完毕
            for(StatusCodeThread t: threads){
                t.join();
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}







