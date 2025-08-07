package com.tehilat.sidur;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DonationActivity extends AppCompatActivity {

    private EditText amountInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);

        amountInput = findViewById(R.id.amount_input);
        Button buttonDonateRussia = findViewById(R.id.button_donate_russia);
        Button buttonDonateInternational = findViewById(R.id.button_donate_international);

        buttonDonateRussia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount = amountInput.getText().toString();
                if (amount.isEmpty()) {
                    Toast.makeText(DonationActivity.this, "Введите сумму", Toast.LENGTH_SHORT).show();
                    return;
                }
                openRussianPaymentPage(amount);
            }
        });

        buttonDonateInternational.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount = amountInput.getText().toString();
                if (amount.isEmpty()) {
                    Toast.makeText(DonationActivity.this, "Please enter amount", Toast.LENGTH_SHORT).show();
                    return;
                }
                openInternationalPaymentPage(amount);
            }
        });
    }

    private void openRussianPaymentPage(String amount) {
        // Пример: ссылка на форму ЮKassa / CloudPayments / Тинькофф
        String url = "https://your-russian-payment-page.com/donate?amount=" + amount;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private void openInternationalPaymentPage(String amount) {
        // Пример: ссылка на PayPal / Stripe / BuyMeACoffee
        String url = "https://your-international-payment-page.com/donate?amount=" + amount;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}
