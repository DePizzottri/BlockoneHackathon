import {Component} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import * as Rx from 'rxjs/Rx';
import 'rxjs/add/operator/map'
import { Http, URLSearchParams } from '@angular/http';
import { XHRBackend, RequestOptions} from '@angular/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
// import * as models from "../swagger/model/models";
// import * as api from "../swagger/api/api";
//import {BACKEND} from '../backend-address';

import {NgbDateStruct, NgbTimeStruct} from '@ng-bootstrap/ng-bootstrap';
//import {HttpWithAuth, userName} from "../auth/auth.service";

export interface Transaction {
    hash: string
    from: string
    to: string
    amount: string
    fee: string
  }

@Component({
  selector: 'transactions',
  templateUrl: './transactions.component.html',
  styleUrls: ['./transactions.component.css']
})

export class TransactionsComponent {
  transactions: Transaction[] = [
    // {
    //     hash:"hash1",
    //     from:"from1",
    //     to:"to1",
    //     amount:"amount1",
    //     fee:"asd"
    // },
    // {
    //     hash:"hash2",
    //     from:"from2",
    //     to:"to2",
    //     amount:"amount2",
    //     fee:"asd"
    // },
    // {
    //     hash:"hash3",
    //     from:"from3",
    //     to:"to3",
    //     amount:"amount3",
    //     fee:"asd"
    // }
  ]

  balance: string = ""
  address: string = ""

  constructor(public http: Http) {
  }

  getBalance() {
    this.http.get('http://localhost:8080/balance?addr='+this.address).map((res) => {
       //console.log(res)
       this.balance = res.json().balance;
    }).subscribe();
  }

  getTransactions() {
    //console.log("get txs")
    this.http.get('http://localhost:8080/txs?addr='+this.address).map((res) => {
       //console.log(res)
       this.transactions = res.json();
    }).subscribe();

    this.getBalance()
  }



  ngOnInit() {

  }
}
