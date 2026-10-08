import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.*;

public class FashionAI {
    static final int PORT = 8080;
    static final List<Product> products = new ArrayList<>();

    static class Product {
        String name, category, colour, direction, stage, risk, explanation;
        int score, search, social, bestseller, vision, leadTime, window, trend, longevity, gap, margin, sentiment;
        Product(String n,String c,String col,int sc,String dir,String st,String risk,int s,int so,int b,int v,int lt,int w,String ex,int lon,int gap,int mar,int sent){
            name=n;category=c;colour=col;score=sc;direction=dir;stage=st;this.risk=risk;search=s;social=so;bestseller=b;vision=v;leadTime=lt;window=w;explanation=ex;trend=sc;longevity=lon;this.gap=gap;margin=mar;sentiment=sent;
        }
    }
    public static void main(String[] args) throws Exception {
        seed();
        HttpServer server=HttpServer.create(new InetSocketAddress(PORT),0);
        server.createContext("/", FashionAI::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("FashionAI running at http://localhost:"+PORT);
    }
    static void seed(){
        products.add(new Product("Oversized Black T-shirt","T-shirt","Black",92,"RISING","Peak approaching","LOW",94,91,87,96,18,55,"Search and social demand are rising while the production window remains comfortable.",88,84,86,90));
        products.add(new Product("Beige Cargo Pants","Cargo","Beige",86,"RISING","Rising","LOW",85,84,82,91,25,70,"Strong search momentum and a longer trend window create a good production opportunity.",82,79,81,87));
        products.add(new Product("Linen Overshirt","Shirt","Cream",79,"RISING","Rising","LOW",78,76,75,88,20,80,"Steady demand and a long expected window make this a lower-risk seasonal product.",91,72,84,89));
        products.add(new Product("Pastel Shirt","Shirt","Pastel Blue",67,"STABLE","Stable","MEDIUM",66,64,68,79,17,60,"Interest is stable; differentiation and price positioning are important.",70,65,76,81));
        products.add(new Product("Skinny Jeans","Jeans","Dark Blue",39,"FALLING","Declining","HIGH",37,35,44,72,35,25,"Demand is weakening and the estimated trend window is shorter than production time.",28,41,61,58));
    }
    static void handle(HttpExchange ex) throws IOException {
        addCors(ex);
        String path=ex.getRequestURI().getPath();
        try {
            if("GET".equals(ex.getRequestMethod()) && path.equals("/")) { sendFile(ex,"index.html","text/html"); return; }
            if("GET".equals(ex.getRequestMethod()) && path.equals("/style.css")) { sendFile(ex,"style.css","text/css"); return; }
            if(path.equals("/api/dashboard")) json(ex,dashboard()); 
            else if(path.equals("/api/trends")) json(ex,trends());
            else if(path.equals("/api/products")) json(ex,productList());
            else if(path.equals("/api/reviews")) json(ex,reviews());
            else if(path.equals("/api/reviews/analyze") && "POST".equals(ex.getRequestMethod())) json(ex,analyze(readBody(ex)));
            else if(path.equals("/api/pricing") && "POST".equals(ex.getRequestMethod())) json(ex,pricing(readBody(ex)));
            else if(path.equals("/api/opportunities")) json(ex,opportunities());
            else if(path.equals("/api/simulate")) json(ex,simulate(ex.getRequestURI().getQuery()));
            else if(path.equals("/api/copilot") && "POST".equals(ex.getRequestMethod())) json(ex,copilot(readBody(ex)));
            else { ex.sendResponseHeaders(404,0); ex.getResponseBody().close(); }
        } catch(Exception e){ send(ex,"{\"error\":\""+esc(e.getMessage())+"\"}",500,"application/json"); }
    }
    static String dashboard(){
        return "{\"trendScore\":82,\"topOpportunity\":\"Oversized T-shirt\",\"topReviewIssue\":\"Fit\",\"risk\":\"LOW\",\"trends\":"+trends()+",\"alerts\":[{\"level\":\"HIGH\",\"title\":\"Trend opportunity detected\",\"text\":\"Oversized Black T-shirt has a 92/100 opportunity score.\"},{\"level\":\"MEDIUM\",\"title\":\"Inventory caution\",\"text\":\"Avoid aggressive production of Skinny Jeans while the trend is falling.\"},{\"level\":\"LOW\",\"title\":\"Customer insight\",\"text\":\"Fit is currently the most frequent complaint bucket.\"}]}";
    }
    static String trends(){
        StringBuilder b=new StringBuilder("[");
        for(int i=0;i<products.size();i++){Product p=products.get(i); if(i>0)b.append(",");
            b.append("{\"name\":\"").append(esc(p.name)).append("\",\"category\":\"").append(p.category).append("\",\"colour\":\"").append(p.colour).append("\",\"score\":").append(p.score).append(",\"direction\":\"").append(p.direction).append("\",\"stage\":\"").append(p.stage).append("\",\"risk\":\"").append(p.risk).append("\",\"search\":").append(p.search).append(",\"social\":").append(p.social).append(",\"bestseller\":").append(p.bestseller).append(",\"vision\":").append(p.vision).append(",\"leadTime\":").append(p.leadTime).append(",\"window\":").append(p.window).append(",\"explanation\":\"").append(esc(p.explanation)).append("\"}");
        } return b+"]";
    }
    static String productList(){
        StringBuilder b=new StringBuilder("[");
        for(int i=0;i<products.size();i++){if(i>0)b.append(",");b.append("{\"name\":\"").append(esc(products.get(i).name)).append("\"}");}return b+"]";
    }
    static String reviews(){
        return "{\"buckets\":[{\"category\":\"Fit\",\"percent\":32},{\"category\":\"Fabric feel\",\"percent\":21},{\"category\":\"Colour mismatch\",\"percent\":17},{\"category\":\"Delivery delay\",\"percent\":13},{\"category\":\"Praise\",\"percent\":17}],\"actions\":[{\"category\":\"Fit\",\"action\":\"Review size measurements and improve the size chart, especially medium sizing.\"},{\"category\":\"Fabric feel\",\"action\":\"Compare fabric composition and consider a softer material for the next batch.\"},{\"category\":\"Colour mismatch\",\"action\":\"Improve product photography and describe colours more precisely.\"},{\"category\":\"Delivery delay\",\"action\":\"Review dispatch SLA and communicate realistic delivery dates.\"}]}";
    }
    static String analyze(String body){
        String text=lower(extract(body,"text"));
        String cat="Praise",sent="Positive",action="Keep the current product strengths and monitor repeat praise.";
        if(has(text,"size","fit","tight","loose","small","large")){cat="Fit";sent="Negative";action="Review size measurements and improve the size chart.";}
        else if(has(text,"fabric","material","cloth","rough","soft","comfortable")){cat="Fabric feel";sent=text.contains("comfortable")||text.contains("soft")?"Positive":"Negative";action="Use fabric feedback to guide the next production batch.";}
        else if(has(text,"colour","color","shade","different from")){cat="Colour mismatch";sent="Negative";action="Improve colour representation in product images and descriptions.";}
        else if(has(text,"delivery","late","shipping","arrived")){cat="Delivery delay";sent="Negative";action="Review dispatch and delivery timelines.";}
        return "{\"category\":\""+cat+"\",\"sentiment\":\""+sent+"\",\"confidence\":93,\"action\":\""+esc(action)+"\"}";
    }
    static String pricing(String body){
        double current=num(body,"currentPrice",699), low=num(body,"competitorLow",649), high=num(body,"competitorHigh",799), stock=num(body,"stock",120);
        String product=extract(body,"product"); Product p=find(product);
        int score=50;
        if(p!=null) score=(p.score + (stock>150?8:stock<40?12:0) + 65)/2;
        double center=(low+high)/2;
        if(p!=null && "RISING".equals(p.direction)) center*=1.04;
        if(p!=null && "FALLING".equals(p.direction)) center*=.92;
        if(stock>150) center*=.94; else if(stock<40) center*=1.05;
        double recLow=Math.max(99,center-35), recHigh=center+35;
        String action=score>=78?"Strong position: consider pricing near the upper half of the competitor range.":score>=60?"Maintain a competitive mid-range price and monitor stock.":"Use a more aggressive price to reduce inventory risk.";
        return "{\"low\":"+Math.round(recLow)+",\"high\":"+Math.round(recHigh)+",\"score\":"+Math.min(99,score)+",\"action\":\""+action+"\",\"reasons\":[{\"label\":\"Competitor range\",\"value\":\""+money(low)+" – "+money(high)+"\"},{\"label\":\"Trend\",\"value\":\""+(p==null?"STABLE":p.direction)+"\"},{\"label\":\"Stock\",\"value\":\""+Math.round(stock)+" units\"},{\"label\":\"Current price\",\"value\":\""+money(current)+"\"}]}";
    }
    static String opportunities(){
        List<Product> a=new ArrayList<>(products); a.sort((x,y)->Integer.compare(y.score,x.score));
        StringBuilder b=new StringBuilder("[");
        for(int i=0;i<a.size();i++){Product p=a.get(i);if(i>0)b.append(",");
            b.append("{\"product\":\"").append(esc(p.name)).append("\",\"score\":").append(p.score).append(",\"trend\":").append(p.trend).append(",\"longevity\":").append(p.longevity).append(",\"gap\":").append(p.gap).append(",\"margin\":").append(p.margin).append(",\"risk\":\"").append(p.risk).append("\",\"reason\":\"").append(esc(p.explanation)).append("\"}");
        }return b+"]";
    }
    static String simulate(String q){
        String product=param(q,"product"); double price=Double.parseDouble(param(q,"price","699")); Product p=find(product); if(p==null)p=products.get(0);
        double ideal=(p.name.startsWith("Oversized")?699:p.name.contains("Cargo")?749:699);
        double distance=Math.abs(price-ideal)/ideal;
        int demand=(int)Math.max(20,Math.min(98,94-distance*85-(price<ideal?0:Math.max(0,price-ideal)*.035)));
        int margin=(int)Math.max(15,Math.min(95,45+(price-500)*.065));
        String dl=demand>78?"High":"Medium", ml=margin>65?"High":"Medium";
        String decision=(demand>75&&margin>60)?"GOOD":"OPTIMIZE";
        return "{\"demand\":"+demand+",\"demandLabel\":\""+dl+" demand estimate\",\"margin\":"+margin+",\"marginLabel\":\""+ml+" margin index\",\"fit\":"+p.score+",\"fitLabel\":\"Product opportunity fit\",\"decision\":\""+decision+"\",\"reason\":\"Best balance is around "+money(ideal)+" for this product.\"}";
    }
    static String copilot(String body){
        String q=lower(extract(body,"question"));
        if(q.contains("make")||q.contains("manufacture")||q.contains("produce")) return "{\"answer\":\"Based on the current opportunity scores, prioritize <b>Oversized Black T-shirt</b> (92/100), followed by <b>Beige Cargo Pants</b> (86/100). The first has strong demand and an 18-day lead time against an estimated 55-day trend window.\"}";
        if(q.contains("safe")||q.contains("risk")) return "{\"answer\":\"<b>Linen Overshirt</b> is the safest strong opportunity: 79/100 score, LOW risk, a 20-day lead time and an estimated 80-day trend window. Avoid aggressive Skinny Jeans production because its trend is falling.\"}";
        if(q.contains("price")||q.contains("charge")) return "{\"answer\":\"For an <b>Oversized Black T-shirt</b>, competitors cluster around ₹649–₹799. With a rising trend, a starting price around <b>₹699</b> is a sensible position; use the Price Simulator to test alternatives.\"}";
        if(q.contains("review")||q.contains("customer")||q.contains("fix")) return "{\"answer\":\"The largest feedback bucket is <b>Fit (32%)</b>. Prioritize size measurements and the size chart, then address fabric feel (21%) and colour mismatch (17%).\"}";
        return "{\"answer\":\"I can help with <b>what to make, pricing, trend risk and customer feedback</b>. Try asking: ‘What should I manufacture next?’, ‘Which trend is safest?’, or ‘What should I fix from reviews?’\"}";
    }
    static Product find(String n){for(Product p:products)if(p.name.equalsIgnoreCase(n))return p;return null;}
    static boolean has(String s,String... words){for(String w:words)if(s.contains(w))return true;return false;}
    static String lower(String s){return s==null?"":s.toLowerCase(Locale.ROOT);}
    static double num(String b,String k,double def){try{return Double.parseDouble(extract(b,k));}catch(Exception e){return def;}}
    static String extract(String b,String key){Matcher m=Pattern.compile("\""+Pattern.quote(key)+"\"\\s*:\\s*\"([^\"]*)\"").matcher(b);return m.find()?m.group(1):"";}
    static String param(String q,String key){return param(q,key,"");}
    static String param(String q,String key,String def){if(q==null)return def;for(String x:q.split("&")){String[] z=x.split("=",2);if(z.length==2&&URLDecoder.decode(z[0],StandardCharsets.UTF_8).equals(key))return URLDecoder.decode(z[1],StandardCharsets.UTF_8);}return def;}
    static String money(double n){return "₹"+Math.round(n);}
    static String readBody(HttpExchange e)throws IOException{return new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);}
    static void sendFile(HttpExchange e,String file,String type)throws IOException{Path p=Paths.get(file);if(!Files.exists(p)){send(e,"File not found: "+file,404,"text/plain");return;}send(e,Files.readString(p),200,type);}
    static void json(HttpExchange e,String s)throws IOException{send(e,s,200,"application/json");}
    static void send(HttpExchange e,String s,int code,String type)throws IOException{byte[] b=s.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type",type+"; charset=UTF-8");e.getResponseHeaders().set("Access-Control-Allow-Origin","*");e.sendResponseHeaders(code,b.length);try(OutputStream o=e.getResponseBody()){o.write(b);}}
    static void addCors(HttpExchange e){e.getResponseHeaders().set("Access-Control-Allow-Origin","*");e.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");}
    static String esc(String s){if(s==null)return "";return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ");}
}