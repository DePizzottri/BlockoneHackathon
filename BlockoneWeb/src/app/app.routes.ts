import { Routes } from '@angular/router';

//import { CustomersComponent } from './customers/customers.component';
import { TransactionsComponent } from './transactions/transactions.component';
//import {CheckComponent} from './check/check.component';

export const rootRouterConfig: Routes = [
  { path:'transactions', component: TransactionsComponent },

  { path: '', redirectTo: 'transactions', pathMatch: 'full' },

];
