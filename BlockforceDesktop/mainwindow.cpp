#include "mainwindow.h"
#include "ui_mainwindow.h"

#include <QtNetwork/QNetworkReply>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>

#include <QStandardItemModel>

MainWindow::MainWindow(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::MainWindow)
{
    ui->setupUi(this);

    balance_manager = new QNetworkAccessManager();
    QObject::connect(balance_manager, &QNetworkAccessManager::finished,
       this, [=](QNetworkReply *reply) {
           if (reply->error()) {
               qDebug() << reply->errorString();
               return;
           }

           QString answer = reply->readAll();

           QJsonParseError error;
           auto resp = QJsonDocument::fromJson(answer.toLocal8Bit(), &error);

           if(error.error == QJsonParseError::NoError) {
              if(resp.object().find("result") != resp.object().end()) {
                ui->label_5->setText(resp.object()["result"].toString());
              } else {
                  qDebug() << answer;

                  qDebug() <<"No field with name \"result\"";
              }
           } else {
               qDebug() << answer;
               qDebug() << "Parse error:  "<< error.errorString();
           }
        }
    );

    txs_manager = new QNetworkAccessManager();
    QObject::connect(txs_manager, &QNetworkAccessManager::finished,
       this, [=](QNetworkReply *reply) {
           if (reply->error()) {
               qDebug() << reply->errorString();
               return;
           }

           QString answer = reply->readAll();

           QJsonParseError error;
           auto resp = QJsonDocument::fromJson(answer.toLocal8Bit(), &error);

           if(error.error == QJsonParseError::NoError) {
               if(resp.object().find("result") != resp.object().end()) {
                 //ui->label_5->setText(resp.object()["result"].toString());

                 auto arr = resp.object()["result"].toArray();

                 QStandardItemModel *model = new QStandardItemModel(arr.size(), 4, this); //2 Rows and 3 Columns
                 model->setHorizontalHeaderItem(0, new QStandardItem(QString("txHash")));
                 model->setHorizontalHeaderItem(1, new QStandardItem(QString("From")));
                 model->setHorizontalHeaderItem(2, new QStandardItem(QString("To")));
                 model->setHorizontalHeaderItem(3, new QStandardItem(QString("Value")));

                 for(int i = 0; i<arr.size(); ++i) {
                    auto tx = arr[i];
                    model->setItem(i, 0, new QStandardItem(tx.toObject()["hash"].toString()));
                    model->setItem(i, 1, new QStandardItem(tx.toObject()["from"].toString()));
                    model->setItem(i, 2, new QStandardItem(tx.toObject()["to"].toString()));
                    model->setItem(i, 3, new QStandardItem(tx.toObject()["value"].toString()));
                 }
                 ui->tableView->setModel(model);

               } else {
                   qDebug() << answer;
                   qDebug() <<"No field with name \"result\"";
               }


           } else {
               qDebug() << answer;
               qDebug() << "Parce error:  "<< error.errorString();
           }

       }
   );
}

MainWindow::~MainWindow()
{
    delete ui;
}

void MainWindow::on_pushButton_clicked()
{
    //SD8DTQ1NB9CHY4Y8FWAGXEGYRDG6V5FWWM
    //0xddbd2b932c763ba5b1b7ae3b362eac3e8d40121a

    balance_request.setUrl(QUrl("http://api.etherscan.io/api?module=account&action=balance&address=" + ui->lineEdit->text() + "&tag=latest&apikey="+ui->lineEdit_2->text()));
    balance_manager->get(balance_request);

    txs_request.setUrl(QUrl("http://api.etherscan.io/api?module=account&action=txlist&address=" + ui->lineEdit->text() + "&startblock=0&endblock=99999999&sort=asc&apikey=YourApiKey"+ui->lineEdit_2->text()));
    txs_manager->get(txs_request);

    ui->label_5->setText("");
    ui->tableView->setModel(nullptr);
}

#include <QFile>
#include <QTextStream>
#include <QDesktopServices>

void MainWindow::on_pushButton_2_clicked()
{
    auto filename = ui->lineEdit->text() + ".csv";
    QFile fout(filename);
    fout.open(QIODevice::WriteOnly);

    QTextStream sout(&fout);

    sout << "txHash;From;To;Amount"<<endl;

    auto model = static_cast<QStandardItemModel*> (ui->tableView->model());

    for(int i =0; i<model->rowCount(); ++i) {
       sout <<model->item(i, 0)<<";";
       sout <<model->item(i, 1)<<";";
       sout <<model->item(i, 2)<<";";
       sout <<model->item(i, 3)<<";";
       sout << endl;
    }

    ui->label_6->setText(filename + " записан");
    QDesktopServices::openUrl( QUrl::fromLocalFile( filename ) );
}
