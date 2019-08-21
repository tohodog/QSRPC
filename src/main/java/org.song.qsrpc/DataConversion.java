//package org.song.qsrpc;
//
//import com.alibaba.fastjson.JSONObject;
//import io.netty.handler.codec.http.HttpResponseStatus;
//import io.netty.handler.codec.http.QueryStringDecoder;
//
//import java.nio.charset.Charset;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author song
// * @Email vipqinsong@gmail.com
// * @date 2019年3月20日 下午5:04:05
// * <p>
// * http消息和rpc消息转换
// * <p>
// * IRequest <=> Message Response <=> Message
// * <p>
// * 目前用json承载,追求效率可以换别的
// */
//public class DataConversion {
//
//    public static Message Request2Message(IRequest iRequest) {
//        Message message = new Message();
//        JSONObject jsonRequest = new JSONObject();
//        jsonRequest.put("i", iRequest.ip());
//        jsonRequest.put("m", iRequest.method());
//        jsonRequest.put("u", iRequest.uri());
//        jsonRequest.put("h", iRequest.header());
//        jsonRequest.put("b", iRequest.bytesBody());
//        message.setJSONObject(jsonRequest);
//        return message;
//    }
//
//    public static IRequest Message2Request(Message message) {
//        return new RPCRequest(message);
//    }
//
//    public static Message Response2Message(Response response, int id) {
//        Message message = new Message();
//        JSONObject jsonResponse = new JSONObject();
//        jsonResponse.put("c", response.status().code());
//        jsonResponse.put("h", response.headers());
//        Object object = response.contentBody();
//        if (object instanceof String) {
//            jsonResponse.put("b", response.contentBody());
//            jsonResponse.put("t", 0);
//        } else if (object instanceof byte[]) {
//            jsonResponse.put("b", response.contentBody());
//            jsonResponse.put("t", 1);
//        }
//        message.setId(id);
//        message.setJSONObject(jsonResponse);
//        return message;
//    }
//
//    public static Response Message2Response(Message message) {
//        JSONObject jsonResponse = message.getJSONObject();
//        Response.Builder response = Response.Builder(HttpResponseStatus.valueOf(jsonResponse.getIntValue("c")));
//        JSONObject headers = jsonResponse.getJSONObject("h");
//        for (String key : headers.keySet())
//            response.header(key, headers.getString(key));
//        if (jsonResponse.getIntValue("t") == 0)
//            response.stringBody(jsonResponse.getString("b"));
//        else
//            response.bytesBody(jsonResponse.getBytes("b"));
//        return response.build();
//    }
//
//    public static class RPCRequest implements IRequest {
//
//        private JSONObject jsonRequest;
//        private int id;
//
//        RPCRequest(Message message) {
//            this.jsonRequest = message.getJSONObject();
//            id = message.getId();
//        }
//
//        @Override
//        public String ip() {
//            return jsonRequest.getString("i");
//        }
//
//        @Override
//        public String method() {
//            return jsonRequest.getString("m");
//
//        }
//
//        @Override
//        public String uri() {
//            return jsonRequest.getString("u");
//        }
//
//        @Override
//        public Map<String, String> header() {
//            Map<String, String> map = new HashMap<>();
//            JSONObject headers = jsonRequest.getJSONObject("h");
//            if (headers != null)
//                for (String key : headers.keySet()) {
//                    map.put(key, headers.getString(key));
//                }
//            return map;
//        }
//
//        private byte[] bytesBody;
//
//        @Override
//        public byte[] bytesBody() {
//            if (bytesBody == null)
//                bytesBody = jsonRequest.getBytes("b");
//            return bytesBody;
//        }
//
//        private String path;
//        private QueryStringDecoder queryDecoder;
//
//        @Override
//        public String path() {
//            if (path == null) {
//                if (queryDecoder == null)
//                    queryDecoder = new QueryStringDecoder(uri());
//                path = queryDecoder.path();
//            }
//            return path;
//        }
//
//        @Override
//        public String header(CharSequence key) {
//            return jsonRequest.getJSONObject("h") != null ? jsonRequest.getJSONObject("h").getString(key.toString())
//                    : null;
//        }
//
//        @Override
//        public boolean isKeepAlive() {
//            return true;
//        }
//
//        @Override
//        public Charset charset() {
//            return Util.charset(contentType());
//        }
//
//        @Override
//        public String contentType() {
//            return header("content-type");
//        }
//
//        @Override
//        public String stringBody() {
//            if (bytesBody() != null)
//                return new String(bytesBody(), charset());
//            return null;
//        }
//
//        // application/x-www-form-urlencoded 解析后数据
//        private Map<String, String> parame;
//
//        @Override
//        public String param(String key) {
//            return param().get(key);
//        }
//
//        @Override
//        public Map<String, String> param() {
//            if (parame == null) {
//                switch (method()) {
//                    case "POST":
//                    case "PUT":
//                    case "PATCH":
//                        parame = Util.parserForm(stringBody());
//                        // 兼容下POST/PUT也可以拿到url带的数据
//                        if (!parame.isEmpty())
//                            break;
//                    case "GET":
//                    case "DELETE":
//                    case "HEAD":
//                    default:
//                        if (queryDecoder == null)
//                            queryDecoder = new QueryStringDecoder(uri());
//                        Map<String, List<String>> map = queryDecoder.parameters();
//                        parame = new HashMap<>();
//                        for (String key : map.keySet()) {
//                            List<String> list = map.get(key);
//                            if (list != null && list.size() > 0)
//                                parame.put(key, map.get(key).get(0));
//                        }
//
//                }
//            }
//            return parame;
//        }
//        // multipart/form-data 解析后数据
//
//        @Override
//        public Map<String, Object> multipart() {
//            // NO SUPPOT
//            return null;
//        }
//
//        @Override
//        public Object multipart(String key) {
//            // NO SUPPOT
//            return null;
//        }
//
//        @Override
//        public String toString() {
//            String space = " | ";
//            StringBuilder sb = new StringBuilder("Request==> ");
//            sb.append(method());
//            sb.append(space);
//            sb.append(uri());
//            sb.append(space);
//            sb.append(ip());
//            sb.append(space);
//            sb.append(id);
//            sb.append(space);
//            sb.append("Headers->");
//            sb.append(header());
//            if (bytesBody != null && bytesBody.length > 0) {
//                sb.append("\n");
//                sb.append(stringBody());
//            }
//            return sb.toString();
//        }
//
//    }
//}
