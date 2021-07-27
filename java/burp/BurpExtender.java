package burp;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

public class BurpExtender implements IBurpExtender, IContextMenuFactory
{
    private IExtensionHelpers helpers;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
    {
        // set our extension name
        callbacks.setExtensionName("UploadFile Request");

        // save the helpers for later
        this.helpers = callbacks.getHelpers();
        callbacks.registerContextMenuFactory(this);//注册菜单
    }

    //添加右击菜单
    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
//        if(invocation.getToolFlag() == IBurpExtenderCallbacks.TOOL_REPEATER) {//仅在repeater下生效
            List<JMenuItem> listMenuItems = new ArrayList<JMenuItem>();
            JMenuItem menuItem = new JMenuItem("Upload simple file");

//            JMenu jm = new JMenu("upload extender");
//            jm.add(menuItem);
            menuItem.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    IHttpRequestResponse iReqResp = invocation.getSelectedMessages()[0];
                    try{
                        byte[] request = iReqResp.getRequest();
                        if(Objects.equals(helpers.analyzeRequest(request).getMethod(), "GET")){
                            request = helpers.toggleRequestMethod(request);//将GET转换成POST（添加content-type、content-length头）
                        }

                        IRequestInfo requestInfo = helpers.analyzeRequest(request);

                        //修改content-type为文件上传的形式
                        List<String> headers;
                        headers = requestInfo.getHeaders();

                        Iterator<String> iter = headers.iterator();
                        while (iter.hasNext()){
                            if(iter.next().contains("Content-Type")){
                                iter.remove();
                            }
                        }

                        String boundary_random_str = "---------------------------"+getRandomString(29);
                        headers.add("Content-Type: multipart/form-data; boundary="+boundary_random_str);
                        boundary_random_str = "--"+boundary_random_str;

                        //将post参数改写成boundary的形式
                        int bodyOffset = requestInfo.getBodyOffset();
                        String body = new String(request, bodyOffset, request.length - bodyOffset, "UTF-8");
                        String uploadbody = "";
                        Map<String,String> params = splitQuery(body);//获取参数
                        for (String key : params.keySet()){
                            //key -> params.get(key)
                            uploadbody += boundary_random_str+"\n";
                            String tmpbody = generateOneBoundary(key, params.get(key));
                            uploadbody += tmpbody;
                        }

                        //增加一个上传的文件
                        uploadbody += boundary_random_str+"\n";
                        String tmpbody = generateOneBoundary("file1", "<?php phpinfo();?>", "ka1n4t.php");
                        uploadbody += tmpbody;

                        //增加submit
                        uploadbody += boundary_random_str+"\n";
                        tmpbody = generateOneBoundary("submit", "Submit");
                        uploadbody += tmpbody;
                        uploadbody += boundary_random_str+"--\n";

                        //设置最终的request
                        request = helpers.buildHttpMessage(headers, uploadbody.getBytes());
                        iReqResp.setRequest(request);

                    }catch (Exception ee){
                        ee.printStackTrace();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {

                }

                @Override
                public void mouseExited(MouseEvent e) {

                }
            });

            listMenuItems.add(menuItem);
            return listMenuItems;
//        }

    }

    private static String generateOneBoundary(String name, String value){
        try{
            String template =
                    "Content-Disposition: form-data; name=\""+name+"\"\n" +
                            "Content-Type: text/plain; charset=UTF-8\n" +
//                    "Content-Transfer-Encoding: 8bit\n" +
                            "\n" +
                            value+"\n";

            return template;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    private static String generateOneBoundary(String name, String content, String filename){
        try{
            String template =
                    "Content-Disposition: form-data; name=\""+name+"\"; filename=\""+filename+"\"\n" +
                            "Content-Type: text/plain; charset=UTF-8\n" +
//                    "Content-Transfer-Encoding: 8bit\n" +
                            "\n" +
                            content+"\n";

            return template;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static Map<String,String> splitQuery(String body) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        List<String> pairs = Arrays.asList(body.split("&"));

        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!query_pairs.containsKey(key)) {
                query_pairs.put(key, "");
            }
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : "";
            query_pairs.put(key,value.trim());
        }
        return query_pairs;
    }

    public static String getRandomString(int length){
//        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String str="0123456789";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
//            int number=random.nextInt(62);
            int number=random.nextInt(10);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}
