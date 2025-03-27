package com.mobihub.utils.email

private const val EMAIL_TEMPLATE = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                /* Inline styles are recommended for HTML emails */
                body {
                    font-family: Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    background-color: #f4f4f4;
                    color: #333333;
                }
                .email-container {
                    max-width: 600px;
                    margin: 20px auto;
                    background-color: #ffffff;
                    padding: 20px;
                    border-radius: 8px;
                    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
                }
                .button {
                    display: inline-block;
                    padding: 20px 20px;
                    margin-top: 20px;
                    background-color: #007bff;
                    color: #ffffff;
                    text-decoration: none;
                    border-radius: 5px;
                }
                .button:hover {
                    background-color: #0056b3;
                }
            </style>
        </head>
        {{body}}
        </html>
    """

/**
 * Represents an email message.
 *
 * This class contains the recipients, subject, and body for the message.
 *
 * @property to A list of email addresses to which the message will be sent.
 * @property subject The subject line of the email.
 * @property content The content of the email, typically in plain text or HTML format.
 *
 * @author mobiHub
 */
class EmailMessage(val to: List<String>, val subject: String
) {
    var content: String = "";

    private val htmlEmailTemplate = EMAIL_TEMPLATE;

    /**
     * This constructor for this class places the received message content in the body section of the html template.
     *
     * @param to The list of recipients
     * @param subject The subject for this email message
     * @param bodyToAdd The content for the body of the HTML message.
     */
    constructor(to: List<String>, subject: String, bodyToAdd: String) : this(to, subject) {
        this.content = htmlEmailTemplate.replace("{{body}}", bodyToAdd);
    }
}