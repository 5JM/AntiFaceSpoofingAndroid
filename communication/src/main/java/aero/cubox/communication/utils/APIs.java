package aero.cubox.communication.utils;

public class APIs {
//    private String BASE_URL = "https://shinhaninvest-api.cubox-pd.com/v1/";
    private String BASE_URL = "";

    public void setBaseUrl(String _url){
        BASE_URL = checkPattern(_url);
    }
    public String getBaseUrl(){
        return BASE_URL;
    }
    private String checkPattern(String str){
        char tt = str.charAt(str.length()-1);
        if(tt == '/'){
            return str;
        }else{
            return str.concat("/");
        }
    }
}
