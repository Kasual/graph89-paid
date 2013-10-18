/* Hey EMACS -*- linux-c -*- */
/* $Id: romcalls.h 2268 2006-11-06 17:18:51Z roms $ */

/*  TiEmu - Tiemu Is an EMUlator
 *
 *  Copyright (c) 2000-2001, Thomas Corvazier, Romain Lievin
 *  Copyright (c) 2001-2003, Romain Lievin
 *  Copyright (c) 2003, Julien Blache
 *  Copyright (c) 2004, Romain Li�vin
 *  Copyright (c) 2005, Romain Li�vin, Kevin Kofler
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street - Fifth Floor, Boston, MA 02110-1301, USA.
 */

/*
  Breakpoint definitions
*/

#ifndef __ROMCALLS__
#define __ROMCALLS__

#include <stdio.h>
#include <stdint.h>
#ifndef FLOATFORMAT_H /* Don't include glib.h when included from GDB. */
#include <glib.h>
#endif

#define NMAX_ROMCALLS	0x800

/* Types */

typedef struct
{
	int			id;
    uint32_t    addr;
    char*       name;
} ROM_CALL;

/* Functions */

void romcalls_get_table_infos(uint32_t *base, uint32_t *size);
void romcalls_get_symbol_address(int id, uint32_t *addr);
int romcalls_load(const char* filename);
int romcalls_is_loaded(void);

#ifndef FLOATFORMAT_H
GList* romcalls_sort_by_id(void);
GList* romcalls_sort_by_addr(void);
GList* romcalls_sort_by_name(void);
GList* romcalls_sort_by_iname(void);
#endif

int romcalls_is_addr(uint32_t addr);
int romcalls_is_name(const char *name);

const char* romcalls_get_name(int id);
uint32_t romcalls_get_addr(int id);

const char* ercodes_get_name(unsigned id);

#define ROMCALL_ID(elt)		(((ROM_CALL *)(elt->data))->id)
#define ROMCALL_NAME(elt)	(((ROM_CALL *)(elt->data))->name)
#define ROMCALL_ADDR(elt)	(((ROM_CALL *)(elt->data))->addr)

#endif
