# -*- coding: utf-8 -*-
# Generated by Django 1.11.3 on 2018-04-19 13:06
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('app0', '0013_issue_status'),
    ]

    operations = [
        migrations.AddField(
            model_name='file',
            name='Content',
            field=models.TextField(default=''),
        ),
    ]
