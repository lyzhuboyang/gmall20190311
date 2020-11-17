import org.junit.Test;



public class Test1 {

    @Test
    public void test(){
        //
        String domains = "";
        System.out.println(domains.length());
        String serverName = "http://item.gmall.com/37.html/";
        serverName = serverName.toLowerCase();
        System.out.println("1"+serverName);//http://item.gmall.com/37.html/
        serverName = serverName.substring(7);
        System.out.println("2"+serverName);//item.gmall.com/37.html/
        final int end = serverName.indexOf("/");
        serverName = serverName.substring(0, end);
        System.out.println("3"+serverName);//item.gmall.com
        serverName.split("\\.");
        System.out.println("4"+serverName);//item.gmall.com
        //-1
        System.out.println("item.gmall.com".indexOf(":"));
    }




}
