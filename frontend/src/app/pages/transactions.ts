import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { Account, ApiService, errorMessage, Transaction, TransactionRequest, TransferRequest } from '../api.service';

@Component({
  selector: 'app-transactions',
  imports: [FormsModule, DatePipe, DecimalPipe, MatTableModule, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatSelectModule],
  template: `
    <div class="cards">
      <mat-card>
        <mat-card-header><mat-card-title>Nouvelle transaction</mat-card-title></mat-card-header>
        <mat-card-content>
          <form class="column" (ngSubmit)="createTransaction()">
            <mat-form-field>
              <mat-label>Compte</mat-label>
              <mat-select name="accountId" [(ngModel)]="payment.accountId" (selectionChange)="syncCurrency()" required>
                @for (a of accounts(); track a.id) {
                  <mat-option [value]="a.id">{{ a.customerReference }} ({{ a.currency }}, {{ a.status }})</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field>
              <mat-label>Montant</mat-label>
              <input matInput type="number" name="amount" [(ngModel)]="payment.amount" required />
            </mat-form-field>
            <mat-form-field>
              <mat-label>Devise</mat-label>
              <input matInput name="currency" [(ngModel)]="payment.currency" maxlength="3" required />
            </mat-form-field>
            <mat-form-field>
              <mat-label>Catégorie marchand</mat-label>
              <input matInput name="merchantCategory" [(ngModel)]="payment.merchantCategory" required />
            </mat-form-field>
            <mat-form-field>
              <mat-label>Pays</mat-label>
              <input matInput name="country" [(ngModel)]="payment.country" maxlength="2" required />
            </mat-form-field>
            <button mat-flat-button type="submit">Payer</button>
          </form>
        </mat-card-content>
      </mat-card>

      <mat-card>
        <mat-card-header><mat-card-title>Virement</mat-card-title></mat-card-header>
        <mat-card-content>
          <form class="column" (ngSubmit)="createTransfer()">
            <mat-form-field>
              <mat-label>Compte à débiter</mat-label>
              <mat-select name="fromAccountId" [(ngModel)]="transferForm.fromAccountId" required>
                @for (a of accounts(); track a.id) {
                  <mat-option [value]="a.id">{{ a.customerReference }} ({{ a.currency }}, {{ a.balance }})</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field>
              <mat-label>Compte à créditer</mat-label>
              <mat-select name="toAccountId" [(ngModel)]="transferForm.toAccountId" required>
                @for (a of accounts(); track a.id) {
                  <mat-option [value]="a.id">{{ a.customerReference }} ({{ a.currency }}, {{ a.balance }})</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field>
              <mat-label>Montant</mat-label>
              <input matInput type="number" name="amount" [(ngModel)]="transferForm.amount" required />
            </mat-form-field>
            <mat-form-field>
              <mat-label>Devise</mat-label>
              <input matInput name="currency" [(ngModel)]="transferForm.currency" maxlength="3" required />
            </mat-form-field>
            <button mat-flat-button type="submit">Virer</button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>

    <mat-card>
      <mat-card-header><mat-card-title>Transactions</mat-card-title></mat-card-header>
      <mat-card-content>
        <table mat-table [dataSource]="transactions()">
          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef>Date</th>
            <td mat-cell *matCellDef="let t">{{ t.createdAt | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
          </ng-container>
          <ng-container matColumnDef="account">
            <th mat-header-cell *matHeaderCellDef>Compte</th>
            <td mat-cell *matCellDef="let t">{{ accountLabel(t.accountId) }}</td>
          </ng-container>
          <ng-container matColumnDef="type">
            <th mat-header-cell *matHeaderCellDef>Type</th>
            <td mat-cell *matCellDef="let t">{{ t.type }}</td>
          </ng-container>
          <ng-container matColumnDef="amount">
            <th mat-header-cell *matHeaderCellDef>Montant</th>
            <td mat-cell *matCellDef="let t">{{ t.amount | number: '1.2-2' }} {{ t.currency }}</td>
          </ng-container>
          <ng-container matColumnDef="merchantCategory">
            <th mat-header-cell *matHeaderCellDef>Catégorie</th>
            <td mat-cell *matCellDef="let t">{{ t.merchantCategory }}</td>
          </ng-container>
          <ng-container matColumnDef="country">
            <th mat-header-cell *matHeaderCellDef>Pays</th>
            <td mat-cell *matCellDef="let t">{{ t.country }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"></tr>
        </table>
      </mat-card-content>
    </mat-card>
  `,
})
export class TransactionsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly columns = ['createdAt', 'account', 'type', 'amount', 'merchantCategory', 'country'];
  protected readonly accounts = signal<Account[]>([]);
  protected readonly transactions = signal<Transaction[]>([]);
  protected payment: TransactionRequest = this.emptyPayment();
  protected transferForm: TransferRequest = { fromAccountId: '', toAccountId: '', amount: 0, currency: 'EUR' };

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.api.getAccounts().subscribe({ next: (a) => this.accounts.set(a), error: (e) => this.showError(e) });
    this.api.getTransactions().subscribe({
      next: (t) => this.transactions.set([...t].sort((x, y) => y.createdAt.localeCompare(x.createdAt))),
      error: (e) => this.showError(e),
    });
  }

  protected syncCurrency(): void {
    const account = this.accounts().find((a) => a.id === this.payment.accountId);
    if (account) {
      this.payment.currency = account.currency;
    }
  }

  protected accountLabel(id: string): string {
    return this.accounts().find((a) => a.id === id)?.customerReference ?? id;
  }

  protected createTransaction(): void {
    this.api.createTransaction(this.payment).subscribe({
      next: () => {
        this.snackBar.open('Transaction enregistrée', 'OK', { duration: 3000 });
        this.payment = this.emptyPayment();
        this.load();
      },
      error: (e) => this.showError(e),
    });
  }

  protected createTransfer(): void {
    this.api.transfer(this.transferForm).subscribe({
      next: () => {
        this.snackBar.open('Virement effectué', 'OK', { duration: 3000 });
        this.load();
      },
      error: (e) => this.showError(e),
    });
  }

  private emptyPayment(): TransactionRequest {
    return { accountId: '', amount: 0, currency: 'EUR', merchantCategory: 'GROCERY', country: 'FR' };
  }

  private showError(error: HttpErrorResponse): void {
    this.snackBar.open(errorMessage(error), 'Fermer', { duration: 6000 });
  }
}
