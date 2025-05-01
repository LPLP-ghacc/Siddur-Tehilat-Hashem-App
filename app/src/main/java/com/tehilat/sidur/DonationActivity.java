package com.tehilat.sidur;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DonationActivity extends AppCompatActivity {

    private Spinner recipientSpinner;
    private EditText amountInput;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);

        // Инициализация элементов
        recipientSpinner = findViewById(R.id.recipient_spinner);
        amountInput = findViewById(R.id.amount_input);
        submitButton = findViewById(R.id.submit_donation_button);

        // Настройка Spinner для выбора получателя
        String[] recipients = {"Синагога", "Авторы приложения"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, recipients);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recipientSpinner.setAdapter(adapter);

        // Обработчик нажатия на кнопку отправки
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String recipient = recipientSpinner.getSelectedItem().toString();
                String amountText = amountInput.getText().toString();

                if (amountText.isEmpty()) {
                    Toast.makeText(DonationActivity.this, "Пожалуйста, введите сумму", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double amount = Double.parseDouble(amountText);
                    if (amount <= 0) {
                        Toast.makeText(DonationActivity.this, "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Здесь можно добавить логику для обработки пожертвования
                    // Например, отправка данных на сервер или открытие платежной системы
                    String message = String.format("Спасибо за ваше пожертвование в размере %.2f для %s!", amount, recipient);
                    Toast.makeText(DonationActivity.this, message, Toast.LENGTH_LONG).show();

                    // Закрываем Activity после успешной отправки
                    finish();

                } catch (NumberFormatException e) {
                    Toast.makeText(DonationActivity.this, "Пожалуйста, введите корректную сумму", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}