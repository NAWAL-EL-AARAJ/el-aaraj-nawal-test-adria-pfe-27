import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface Account {
  id: string;
  customerReference: string;
  status: 'ACTIVE' | 'BLOCKED';
  currency: string;
  balance: number;
}

export interface AccountRequest {
  customerReference: string;
  currency: string;
  balance: number;
}

export interface Transaction {
  id: string;
  accountId: string;
  amount: number;
  currency: string;
  merchantCategory: string;
  country: string;
  type: string;
  createdAt: string;
}

export interface TransactionRequest {
  accountId: string;
  amount: number;
  currency: string;
  merchantCategory: string;
  country: string;
}

export interface TransferRequest {
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  currency: string;
}

export interface ChatResponse {
  conversationId: string;
  answer: string;
}

/** Toutes les requêtes passent par la gateway. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api';

  getAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.baseUrl}/accounts`);
  }

  createAccount(request: AccountRequest): Observable<Account> {
    return this.http.post<Account>(`${this.baseUrl}/accounts`, request);
  }

  blockAccount(id: string): Observable<Account> {
    return this.http.put<Account>(`${this.baseUrl}/accounts/${id}/block`, {});
  }

  activateAccount(id: string): Observable<Account> {
    return this.http.put<Account>(`${this.baseUrl}/accounts/${id}/activate`, {});
  }

  getTransactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.baseUrl}/transactions`);
  }

  createTransaction(request: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/transactions`, request);
  }

  transfer(request: TransferRequest): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/transactions/transfer`, request);
  }

  chat(conversationId: string, message: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, { conversationId, message });
  }
}

/** Message lisible à partir d'une erreur du backend (ErrorResponse) ou réseau. */
export function errorMessage(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'Gateway injoignable (http://localhost:8080)';
  }
  const body = error.error;
  if (body?.details) {
    return `${body.message} : ${Object.entries(body.details).map(([k, v]) => `${k} ${v}`).join(', ')}`;
  }
  return body?.message ?? `Erreur ${error.status}`;
}
