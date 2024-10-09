package com.projectx.resumebuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatGptService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Autowired
    private PdfService pdfService;

    public String reformatResumeToTemplate(String userResumePath, String templatePath, String outputPdfPath) throws Exception {
        // Extract text from user resume and template resume PDF
        String userResumeText = pdfService.extractTextFromPdf(userResumePath);
        String templateText = pdfService.extractTextFromPdf(templatePath);

        // Initialize HTTP client
        OkHttpClient client = new OkHttpClient();

        // Create ObjectMapper instance for JSON handling
        ObjectMapper objectMapper = new ObjectMapper();

        // Create the JSON body for the request with a contextual reformatting prompt
        ObjectNode jsonBody = objectMapper.createObjectNode();
        jsonBody.put("model", "gpt-4o");

        // Prepare the prompt to reformat the user resume according to the template structure
        String prompt = "Reformat the following resume into the exact structure of the template. "
                +"Only create the sections which are present in given template and remove the other details"
                +"Map the experience, name, skills, and other sections accordingly.\n\n"
                +"Use the headings just as given in template, for example for every company in experience use the client #1,client #2"
                + "Beautify the resume with underlines and bold texts in headlines"
                +"Do not add any '*' in output, include only name without any other details in the top"
                + "Include all companies which are in experience"
                + "Name should be in capital letters"
                + "Do not add any -- symbols"
                + "User Resume:\n" + userResumeText + "\n\n"
                + "Template Resume:\n" + templateText;

        // Create the messages array
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode messageContent = objectMapper.createObjectNode();
        messageContent.put("role", "user");
        messageContent.put("content", prompt);
        messages.add(messageContent);

        // Add messages and other parameters to the JSON body
        jsonBody.set("messages", messages);
        jsonBody.put("temperature", 0.7);

        // Convert the JSON body to a string
        String jsonPayload = objectMapper.writeValueAsString(jsonBody);

        // Create the request body
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),jsonPayload
        );

        // Build the HTTP request
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        // Send the request and process the response
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Unexpected code: " + response);
            }

            // Parse the JSON response and get the reformatted text
            ObjectNode jsonResponse = objectMapper.readValue(response.body().string(), ObjectNode.class);
            String reformattedResumeText = jsonResponse.get("choices").get(0).get("message").get("content").asText();

            // Generate the PDF file with the reformatted resume using enhanced PdfService
            pdfService.createPdf(reformattedResumeText, outputPdfPath);

            return "Resume reformatted successfully as PDF!";
        }
    }
}
