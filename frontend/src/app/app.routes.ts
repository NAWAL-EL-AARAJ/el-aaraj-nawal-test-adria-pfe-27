import { Routes } from '@angular/router';
import { AccountsPage } from './pages/accounts';
import { AssistantPage } from './pages/assistant';
import { TransactionsPage } from './pages/transactions';

export const routes: Routes = [
  { path: 'accounts', component: AccountsPage, title: 'Comptes' },
  { path: 'transactions', component: TransactionsPage, title: 'Transactions' },
  { path: 'assistant', component: AssistantPage, title: 'Assistant' },
  { path: '', redirectTo: 'accounts', pathMatch: 'full' },
  { path: '**', redirectTo: 'accounts' },
];
