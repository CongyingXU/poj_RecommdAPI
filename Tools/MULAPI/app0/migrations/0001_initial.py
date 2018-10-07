# -*- coding: utf-8 -*-
# Generated by Django 1.11.12 on 2018-04-07 03:59
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    initial = True

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='recommendation',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('issuekey', models.CharField(default='Issuekey', max_length=32)),
                ('r_methods', models.TextField()),
                ('l_methods', models.TextField()),
            ],
        ),
    ]
