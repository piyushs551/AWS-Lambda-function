import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class LogEvent implements RequestHandler<SNSEvent, Object> {

  public Object handleRequest(SNSEvent request, Context context) {

    String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

    context.getLogger().log("Invocation started: " + timeStamp);

    context.getLogger().log("1: " + (request == null));

    context.getLogger().log("2: " + (request.getRecords().size()));

    context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());

    timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

    // sending emails

    String to_mail = request.getRecords().get(0).getSNS().getMessage().split(",")[0];
    String token = request.getRecords().get(0).getSNS().getMessage().split(",")[0];
    token = UUID.randomUUID().toString();
    String from_mail = "harshalneelkamal@gmail.com";
    String subject = "Test Email";

    //generate and store to dynamo db...........Starts

    AmazonDynamoDB dbClient = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();

    DynamoDB dynamoDB = new DynamoDB(dbClient);

    Table table =  dynamoDB.getTable("tokenHolder");// update to tokenHolder

    Item item = table.getItem("username",to_mail); // update to username

    if(item == null){
      item = new Item().withPrimaryKey("username",to_mail).with("token", token).with("ttl",((System.currentTimeMillis()/1000 + 1200))); // update to username and token

      PutItemOutcome outcome = table.putItem(item);

      context.getLogger().log("Put successful: " + outcome.toString());

    }else{

      context.getLogger().log("Record Already Present");

      return null;

    }

    //generate asd store to dynamo db...........Ends

    SimpleDateFormat fm = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
    String time = "";
    try {
      time = fm.format(new Date());
    }catch(Exception e){

    }
    String link = "http://example.com/reset?email="+to_mail+"&token="+token;
    String body = "You are receiving this mail because to chose to reset your password." +
            "\n\n Please click the link below to Reset your password"+
            "\nLink: "+ link
            +"\nThis link will only be valid for 20 minuits starting: " + time;

    try{
      AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(Regions.US_EAST_1).build();

      SendEmailRequest emailRequest =  new SendEmailRequest()
              .withDestination(
                      new Destination().withToAddresses(to_mail))
              .withMessage(new Message()
                      .withBody(new Body()
                              .withText(new Content()
                                      .withCharset("UTF-8").withData(body)))
                      .withSubject(new Content()
                              .withCharset("UTF-8").withData(subject)))
              .withSource(from_mail);
      SendEmailResult result = client.sendEmail(emailRequest);

      context.getLogger().log("Request result: " + result.toString());


    }catch (Exception e){
      context.getLogger().log("Exception : " + e.getMessage());
    }

    context.getLogger().log("Invocation completed: " + timeStamp);

    return null;
  }

}
