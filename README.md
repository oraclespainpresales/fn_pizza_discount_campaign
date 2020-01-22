fn_pizza_discount_campaign.git

Serverless Fn function to apply a discount campaign reading data from ATP (Oracle Autonomous Database - Autonomous Transaction Processing). JDK 13 and ojdb8 driver used.
dbwallet.zip file is generated dinamically and download from ATP with OCI cli.

<pre>oci db autonomous-data-warehouse generate-wallet --autonomous-data-warehouse-id ocid1.autonomousdatabase.oc1.eu-frankfurt-1.... --password your_passw0rd --file dbwallet.zip</pre>

To invoke the function you must create a little json object. Example:

<code>echo -n '{"demozone":"madrid","paymentMethod":"amex","pizzaPrice":"21"}' | fn invoke gigis-fn fnpizzadiscountcampaign</code>
