import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiService, errorMessage } from '../api.service';

interface ChatMessage {
  from: 'user' | 'assistant' | 'error';
  text: string;
}

@Component({
  selector: 'app-assistant',
  imports: [FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Assistant</mat-card-title>
        <mat-card-subtitle>Conversation {{ conversationId }}</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <div class="chat">
          @for (m of messages(); track $index) {
            <div class="message" [class]="m.from">{{ m.text }}</div>
          } @empty {
            <p>Exemple : « Liste les comptes » ou « Quel est le solde du compte de CUST-001 ? »</p>
          }
          @if (loading()) {
            <div class="message assistant">…</div>
          }
        </div>
        <form class="row" (ngSubmit)="send()">
          <mat-form-field class="grow">
            <mat-label>Votre message</mat-label>
            <input matInput name="message" [(ngModel)]="message" [disabled]="loading()" autocomplete="off" />
          </mat-form-field>
          <button mat-flat-button type="submit" [disabled]="loading()">Envoyer</button>
          <button mat-button type="button" (click)="reset()">Nouvelle conversation</button>
        </form>
      </mat-card-content>
    </mat-card>
  `,
})
export class AssistantPage {
  private readonly api = inject(ApiService);

  protected conversationId = crypto.randomUUID();
  protected readonly messages = signal<ChatMessage[]>([]);
  protected readonly loading = signal(false);
  protected message = '';

  protected send(): void {
    const text = this.message.trim();
    if (!text || this.loading()) {
      return;
    }
    this.message = '';
    this.add({ from: 'user', text });
    this.loading.set(true);
    this.api.chat(this.conversationId, text).subscribe({
      next: (response) => {
        this.add({ from: 'assistant', text: response.answer });
        this.loading.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.add({ from: 'error', text: errorMessage(e) });
        this.loading.set(false);
      },
    });
  }

  protected reset(): void {
    this.conversationId = crypto.randomUUID();
    this.messages.set([]);
  }

  private add(message: ChatMessage): void {
    this.messages.update((messages) => [...messages, message]);
  }
}
