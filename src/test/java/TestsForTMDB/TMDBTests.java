package TestsForTMDB;

import io.qameta.allure.Description;
import io.restassured.http.ContentType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;

import java.text.SimpleDateFormat;
import java.util.Date;

import java.io.*;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Properties;
import static io.restassured.RestAssured.*;


public class TMDBTests {

    private static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
    static Properties props = new Properties();
    static String apiKey,token,session,user,password,expires;
    static SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
    static int ListToDelete2;

    LocalDate localDate = LocalDate.now();

    public static void setVariables(){
        apiKey = props.getProperty("apiKey");
        token = props.getProperty("token");
        session = props.getProperty("sessionId");
        user = props.getProperty("username");
        password = props.getProperty("password");
        expires = props.getProperty("TokenExpires");
    }

    @BeforeClass
    public static void setUp(){
        baseURI = "https://api.themoviedb.org/3";

        try (InputStream input = new FileInputStream("./src/main/resources/credentials.properties")) {

            props.load(input);

            Date date = new Date();
            Date date1;
            try {
                date1 = sdf.parse(props.getProperty("TokenExpires"));
            }catch (Exception e){
                date1 = null;
            }
            if(date1 == null || date.after(date1) ){
                String token = given().params("api_key",props.getProperty("apiKey"))
                        .when().get("/authentication/token/new")
                        .then().statusCode(200)
                        .and().extract().path("request_token");

                props.setProperty("token",token);


                JSONObject user = new JSONObject();
                user.put("username",props.getProperty("username"));
                user.put("password",props.getProperty("password"));
                user.put("request_token",props.getProperty("token"));

                String expire = given().contentType(ContentType.JSON).accept(ContentType.JSON).body(user.toJSONString())
                        .when().post("/authentication/token/validate_with_login?api_key="+props.getProperty("apiKey"))
                        .then().statusCode(200)
                        .and().extract().path("expires_at");

                props.setProperty("TokenExpires",expire.replaceAll(" UTC",""));

                JSONObject createSession = new JSONObject();
                createSession.put("request_token",props.getProperty("token"));

                String sessionId = given().contentType(ContentType.JSON).accept(ContentType.JSON).body(createSession.toJSONString())
                        .when().post("/authentication/session/new?api_key="+props.getProperty("apiKey"))
                        .then().statusCode(200)
                        .and().extract().path("session_id");

                props.setProperty("sessionId",sessionId);

                OutputStream output = new FileOutputStream("./src/main/resources/credentials.properties");
                props.store(output,null);

                setVariables();
            }else {
                setVariables();
                System.out.println("la secion es valida todavia");
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Test(priority=1)
    public void CreateList(){
        JSONObject list = new JSONObject();
        list.put("name","My firt list");
        list.put("description","This is the description of my seven");
        list.put("language","en");

        ListToDelete2 = given().contentType(ContentType.JSON).accept(ContentType.JSON).body(list.toJSONString())
                .when().post("/list?api_key="+apiKey+"&session_id="+session)
                .then().statusCode(201).log().body()
                .and().extract().path("list_id");

    }
    @Test(priority = 2)
    public void AddMoviesToList(){
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("./src/test/java/Data/data.json")) {
            JSONObject obj2 = (JSONObject) jsonParser.parse(reader);
            JSONArray jArr = (JSONArray) obj2.get("movies");
            Iterator<String> iterator = jArr.iterator();

            while(iterator.hasNext()){
                JSONObject movie = new JSONObject();
                movie.put("media_id",iterator.next());

                given().contentType(ContentType.JSON).accept(ContentType.JSON).body(movie.toJSONString())
                        .when().post(String.format("/list/%s/add_item",ListToDelete2)+"?api_key="+apiKey+"&session_id="+session)
                        .then().statusCode(201).log().body();

            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

    }

    @Test(priority = 3)
    public void GetDetails(){
        given().contentType(ContentType.JSON)
                .when().get("/list/"+ListToDelete2+"?api_key="+apiKey)
                .then().statusCode(200).log().body();
    }

    @Test(priority = 4)
    public void CleanList(){
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("./src/test/java/Data/data.json")) {
            JSONObject obj2 = (JSONObject) jsonParser.parse(reader);
            JSONArray jArr = (JSONArray) obj2.get("movies");
            Iterator<String> iterator = jArr.iterator();

            while(iterator.hasNext()){
                JSONObject movie = new JSONObject();
                movie.put("media_id",iterator.next());

                given().contentType(ContentType.JSON).accept(ContentType.JSON).body(movie.toJSONString())
                        .when().post(String.format("/list/%s/remove_item",ListToDelete2)+"?api_key="+apiKey+"&session_id="+session)
                        .then().statusCode(200).log().body();

            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

    }

    @Test(priority = 5)
    public void deleteList(){
        given().contentType(ContentType.JSON)
                .when().delete("/list/"+ListToDelete2+"?api_key="+apiKey+"&session_id="+session)
                .then().statusCode(500).log().body();
    }

    @Test(priority = 6)
    public void movieDetails(){
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("./src/test/java/Data/data.json")) {
            JSONObject obj2 = (JSONObject) jsonParser.parse(reader);
            JSONArray jArr = (JSONArray) obj2.get("movies");
            Iterator<String> iterator = jArr.iterator();

            while(iterator.hasNext()){
                given().contentType(ContentType.JSON).accept(ContentType.JSON)
                        .when().get(String.format("/movie/%s",iterator.next())+"?api_key="+apiKey)
                        .then().statusCode(200).log().body();
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test(priority = 7)
    public void rateMovie(){
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("./src/test/java/Data/data.json"))
        {
            JSONObject obj = (JSONObject) jsonParser.parse(reader);
            String movieRate = (String) obj.get("movieRate");

            JSONObject value = new JSONObject();
            value.put("value","7.00");

            given().contentType(ContentType.JSON).accept(ContentType.JSON).body(value.toJSONString())
                    .when().post(String.format("/movie/%s/rating",movieRate)+"?api_key="+apiKey+"&session_id="+session)
                    .then().statusCode(201).log().body();

        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }


}
