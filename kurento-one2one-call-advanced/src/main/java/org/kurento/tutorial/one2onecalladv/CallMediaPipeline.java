/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kurento.tutorial.one2onecalladv;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import java.io.IOException;

import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.Continuation;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.StoppedEvent;
import org.kurento.client.VideoInfo;
import org.kurento.client.MediaProfileSpecType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;

/**
 * Media Pipeline (connection of Media Elements) for the advanced one to one video communication.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class CallMediaPipeline {

  private static final Logger log = LoggerFactory.getLogger(CallMediaPipeline.class);
  private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-S");
    public static final String RECORDING_BASE = "file:///mnt/s3/";
  public static final String RECORDING_PATH = df.format(new Date()) + "-";
  public static final String RECORDING_EXT = ".webm";

    public static String FILENAME_CALLER = "";
    public static String FILENAME_CALLEE = "";
    public final String ClaimID = "";
    public final String UserID = "";

  public final MediaPipeline pipeline;
  private final WebRtcEndpoint webRtcCaller;
  public final WebRtcEndpoint webRtcCallee;
  private final RecorderEndpoint recorderCaller;
  private final RecorderEndpoint recorderCallee;
  public static final int HIGH_QUALITY_BITRATE = 2000000; // bps
  public static final int LOW_QUALITY_BITRATE = 240000; // bps

  public CallMediaPipeline(KurentoClient kurento, String from, String to) {

    // Media pipeline
    pipeline = kurento.createMediaPipeline();

    // Media Elements (WebRtcEndpoint, RecorderEndpoint, FaceOverlayFilter)
    webRtcCaller = new WebRtcEndpoint.Builder(pipeline).build();
    webRtcCallee = new WebRtcEndpoint.Builder(pipeline).build();


    System.out.println(RECORDING_PATH + from + RECORDING_EXT);
FILENAME_CALLER = RECORDING_PATH + from + RECORDING_EXT;
    recorderCaller = new RecorderEndpoint.Builder(pipeline, RECORDING_BASE + RECORDING_PATH + from + RECORDING_EXT)
        .build();

    System.out.println(RECORDING_PATH + to + RECORDING_EXT);
FILENAME_CALLEE = RECORDING_PATH + to + RECORDING_EXT;
    recorderCallee = new RecorderEndpoint.Builder(pipeline, RECORDING_BASE + RECORDING_PATH + to + RECORDING_EXT).stopOnEndOfStream().build();
    
    // Connections
    webRtcCaller.setMaxVideoRecvBandwidth(HIGH_QUALITY_BITRATE / 1000); // kbps
    webRtcCaller.setMaxVideoSendBandwidth(HIGH_QUALITY_BITRATE / 1000); // kbps
    webRtcCaller.connect(webRtcCallee);
    webRtcCaller.connect(recorderCaller);
    
    webRtcCallee.setMaxVideoRecvBandwidth(HIGH_QUALITY_BITRATE / 1000); // kbps
    webRtcCallee.setMaxVideoSendBandwidth(HIGH_QUALITY_BITRATE / 1000); // kbps
    webRtcCallee.connect(webRtcCaller);
    webRtcCallee.connect(recorderCallee);
  }

  public void record(final String ClaimID, final String UserID) {
    recorderCaller.record();
    recorderCallee.record(new Continuation<Void>() {

      @Override
      public void onSuccess(Void result) throws Exception {
        System.out.println("recording done ");
        System.out.println("recorderCallee");
        System.out.print(recorderCallee);
        System.out.println("recorderCallee getStats");
        System.out.print(recorderCallee.getStats());
        System.out.println("result");
        System.out.print(result);
        
BasicFileAttributes attr = Files.readAttributes(RECORDING_BASE + RECORDING_PATH + to + RECORDING_EXT, BasicFileAttributes.class);

System.out.println("creationTime: " + attr.creationTime());
System.out.println("lastAccessTime: " + attr.lastAccessTime());
System.out.println("lastModifiedTime: " + attr.lastModifiedTime());

System.out.println("isDirectory: " + attr.isDirectory());
System.out.println("isOther: " + attr.isOther());
System.out.println("isRegularFile: " + attr.isRegularFile());
System.out.println("isSymbolicLink: " + attr.isSymbolicLink());
System.out.println("size: " + attr.size());
        
        
        sendPost(FILENAME_CALLEE, ClaimID, UserID);
  }


      @Override
      public void onError(Throwable cause) throws Exception {
        System.out.println("recording failed");
      }
});
  }


  public void doNext() {
    System.out.println("donext");
  }

  public String generateSdpAnswerForCaller(String sdpOffer) {
    return webRtcCaller.processOffer(sdpOffer);
  }

  public String generateSdpAnswerForCallee(String sdpOffer) {
    return webRtcCallee.processOffer(sdpOffer);
  }

  public MediaPipeline getPipeline() {
    return pipeline;
  }

  public WebRtcEndpoint getCallerWebRtcEp() {
    return webRtcCaller;
  }

  public WebRtcEndpoint getCalleeWebRtcEp() {
    return webRtcCallee;
  }


private final String USER_AGENT = "Mozilla/5.0";

private static void  sendPost(java.lang.String uri, java.lang.String ClaimID, java.lang.String UserID) {

        HttpClient httpclient = new DefaultHttpClient();

        try {

            HttpPost httpPost = new HttpPost("http://www.losscapture.com/v1/data/recordvideo");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("ClaimID", ClaimID));
            nameValuePairs.add(new BasicNameValuePair("UserID", UserID));
            nameValuePairs.add(new BasicNameValuePair("video",uri));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs)); 

            System.out.println("executing request " + httpPost.getRequestLine());
            HttpResponse response = httpclient.execute(httpPost);
            HttpEntity resEntity = response.getEntity();

            System.out.println("----------------------------------------");
            System.out.println(response.getStatusLine());
            if (resEntity != null) {
                System.out.println("Response content length: " + resEntity.getContentLength());
                System.out.println("Chunked?: " + resEntity.isChunked());
                String responseBody = EntityUtils.toString(resEntity);
                System.out.println("Data: " + responseBody);
}
           
           
            EntityUtils.consume(resEntity);
        } 
        catch (Exception e) {
            System.out.println(e);
        }
        finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }



    }
}
