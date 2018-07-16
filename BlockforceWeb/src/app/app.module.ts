import { NgModule } from '@angular/core'
import { RouterModule } from '@angular/router';
import { rootRouterConfig } from './app.routes';
import { AppComponent } from './app.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { HttpModule, Http, XHRBackend, RequestOptions} from '@angular/http';
import { NgbModule,NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { LocationStrategy, HashLocationStrategy } from '@angular/common';

import {TransactionsComponent} from './transactions/transactions.component';


@NgModule({
  declarations: [
    AppComponent,
    TransactionsComponent

  ],
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpModule,
    RouterModule.forRoot(rootRouterConfig, { useHash: true }),
    NgbModule.forRoot()
  ],
  entryComponents: [TransactionsComponent],
  providers: [

  ],
  bootstrap: [ AppComponent ]
})
export class AppModule {

}
