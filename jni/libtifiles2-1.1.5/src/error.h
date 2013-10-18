/* Hey EMACS -*- linux-c -*- */
/* $Id$ */

/*  libtifiles - file format library, a part of the TiLP project
 *  Copyright (C) 1999-2005  Romain Lievin
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
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#ifndef __TIFILES_ERRCODES__
#define __TIFILES_ERRCODES__

/* Error codes must begin at 512 up to 767 */

typedef enum {
	ERR_NO_ERROR = 0,		// No error (returns 0)

	ERR_MALLOC=512,			// Error with malloc
	ERR_FILE_OPEN,			// Unable to open file
	ERR_FILE_CLOSE,			// Unable to close file
	ERR_GROUP_SIZE,			// Group size exceeded (>64KB)
	ERR_BAD_CALC,			// The function does not exist for this calc
	ERR_INVALID_FILE,		// Is not a TI file
	ERR_BAD_FILE,			// Same as above
	ERR_FILE_CHECKSUM,		// Checksum file error
	ERR_FILE_ZIP,			// (Un)Zip internal error
	ERR_UNSUPPORTED,		// Function not supported
	ERR_FILE_IO				// Stream error
} FileError;

#endif
