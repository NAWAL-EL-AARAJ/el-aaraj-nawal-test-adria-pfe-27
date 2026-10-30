import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { Account, AccountRequest, ApiService, errorMessage } from '../api.service';

@Component({
  selector: 'app-accounts',
  imports: [FormsModule, DecimalPipe, MatTableModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  template: `
    <mat-card>
      <mat-card-header><mat-card-title>Nouveau compte</mat-card-title></mat-card-header>
      <mat-card-content>
        <form class="row" (ngSubmit)="create()">
          <mat-form-field>
            <mat-label>Référence client</mat-label>
            <input matInput name="customerReference" [(ngModel)]="form.customerReference" required />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Devise</mat-label>
            <input matInput name="currency" [(ngModel)]="form.currency" maxlength="3" required />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Solde initial</mat-label>
            <input matInput type="number" name="balance" [(ngModel)]="form.balance" />
          </mat-form-field>
          <button mat-flat-button type="submit">Créer</button>
        </form>
      </mat-card-content>
    </mat-card>

    <mat-card>
      <mat-card-header><mat-card-title>Comptes</mat-card-title></mat-card-header>
      <mat-card-content>
        <table mat-table [dataSource]="accounts()">
          <ng-container matColumnDef="customerReference">
            <th mat-header-cell *matHeaderCellDef>Client</th>
            <td mat-cell *matCellDef="let a">{{ a.customerReference }}</td>
          </ng-container>
          <ng-container matColumnDef="id">
            <th mat-header-cell *matHeaderCellDef>Id</th>
            <td mat-cell *matCellDef="let a" class="mono">{{ a.id }}</td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Statut</th>
            <td mat-cell *matCellDef="let a" [class.blocked]="a.status === 'BLOCKED'">{{ a.status }}</td>
          </ng-container>
          <ng-container matColumnDef="currency">
            <th mat-header-cell *matHeaderCellDef>Devise</th>
            <td mat-cell *matCellDef="let a">{{ a.currency }}</td>
          </ng-container>
          <ng-container matColumnDef="balance">
            <th mat-header-cell *matHeaderCellDef>Solde</th>
            <td mat-cell *matCellDef="let a">{{ a.balance | number: '1.2-2' }}</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let a">
              @if (a.status === 'ACTIVE') {
                <button mat-stroked-button (click)="block(a)">Bloquer</button>
              } @else {
                <button mat-stroked-button (click)="activate(a)">Activer</button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"></tr>
        </table>
      </mat-card-content>
    </mat-card>
  `,
})
export class AccountsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly columns = ['customerReference', 'id', 'status', 'currency', 'balance', 'actions'];
  protected readonly accounts = signal<Account[]>([]);
  protected form: AccountRequest = { customerReference: '', currency: 'EUR', balance: 0 };

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.api.getAccounts().subscribe({
      next: (accounts) => this.accounts.set(accounts),
      error: (e) => this.showError(e),
    });
  }

  protected create(): void {
    this.api.createAccount({ ...this.form, currency: this.form.currency.toUpperCase() }).subscribe({
      next: () => {
        this.snackBar.open('Compte créé', 'OK', { duration: 3000 });
        this.form = { customerReference: '', currency: 'EUR', balance: 0 };
        this.load();
      },
      error: (e) => this.showError(e),
    });
  }

  protected block(account: Account): void {
    this.api.blockAccount(account.id).subscribe({ next: () => this.load(), error: (e) => this.showError(e) });
  }

  protected activate(account: Account): void {
    this.api.activateAccount(account.id).subscribe({ next: () => this.load(), error: (e) => this.showError(e) });
  }

  private showError(error: HttpErrorResponse): void {
    this.snackBar.open(errorMessage(error), 'Fermer', { duration: 6000 });
  }
}
